package de.paesserver.intellij.javascript;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.psi.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Arrays;

public class SolidityCodeCompleter extends CompletionContributor {

    private final Logger LOG = Logger.getInstance(this.getClass());

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        doCodeSuggestion(parameters, resultSet);
        // Call super method to ensure other completion contributors are also invoked
        super.fillCompletionVariants(parameters, resultSet);
    }

    private void doCodeSuggestion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        // Get the project and file manager
        Project project = parameters.getPosition().getProject();
        PsiManager psiManager = PsiManager.getInstance(project);
        Editor editor = parameters.getEditor();
        PsiFile currentFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        int offset = editor.getCaretModel().getOffset();
        if (currentFile == null){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't get current file");
            }
            return;
        }

        // Use PSIUtil to get the PSI element at the cursor offset
        PsiElement elementAtCursor = currentFile.findElementAt(offset);
        if (elementAtCursor == null){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't find 'cursor");
            }
            return;
        }
        //Name reference is the reference name of the object. Like myObject.anyMethod(). myObject is the reference name
        //Is needed to know which methods should be completed
        //It should return if the cursor is behind anyMethod. and therefore not referring to a contract
        String nameReference = getNameReference(elementAtCursor);

        String smartContractName = resolveContractVariableReference(nameReference,elementAtCursor);
        if (smartContractName == null){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't resolve contract variable reference");
            }
            return;
        }

        //Not null means that it found a reference to a Smart Contract

        // Retrieve the virtual file corresponding to the Solidity contracts directory
        //FIXME Do not use deprecated method
        VirtualFile solidityDir = project.getBaseDir().findFileByRelativePath("contracts");
        if (solidityDir == null || !solidityDir.isDirectory()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't find 'contracts' directory");
                LOG.debug("Looked for: " + project.getBaseDir() + "/contracts");
            }
            return;
        }

        //Get the smart contract where the reference is referring to
        VirtualFile smartContractFile = solidityDir.findChild(smartContractName + ".sol");
        if (smartContractFile == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't find smart contract files. Looked for " + smartContractName + ".sol");
            }
            return;
        }

        PsiFile psiFile = psiManager.findFile(smartContractFile);
        //Check, if it is actually being recognized as solidity file by the psi tree interpreter or else we cannot resolve the functions
        //At this point the soldiity plugin is needed so that the correct psi tree is being created
        if (psiFile == null || !psiFile.toString().equals("Solidity File")){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't interpret smart contract file. Is the solidity plugin installed?");
            }
            return;
        }

        PsiElement contractDefinition = getContractDefinition(psiFile);
        if (contractDefinition == null){
            return;
        }
        PsiElement[] functionsAndVariables = Arrays.stream(contractDefinition.getChildren()).filter(element -> {
            String type = element.getNode().getElementType().toString();
            return type.contains("FUNCTION_DEFINITION") || type.contains("STATE_VARIABLE_DECLARATION");
        }).toArray(PsiElement[]::new);

        for (PsiElement functionOrVariable : functionsAndVariables) {
            if (functionOrVariable instanceof PsiNamedElement namedElement) {
                ASTNode node = namedElement.getNode();
                String name = namedElement.getName();
                if (name == null){
                    continue;
                }
                if (node.getElementType().toString().equals("FUNCTION_DEFINITION")) {
                    resultSet.addElement(
                            LookupElementBuilder.create(name)
                                    .withIcon(PlatformIcons.METHOD_ICON)
                                    .withTypeText(psiFile.getName(), true)
                    );
                } else if (node.getElementType().toString().equals("STATE_VARIABLE_DECLARATION")) {
                    resultSet.addElement(
                            LookupElementBuilder.create(name)
                                    .withIcon(PlatformIcons.VARIABLE_ICON)
                                    .withTypeText(psiFile.getName(), true)
                    );
                }
            }

        }
        return;
    }

    /**
     * Retrieves the name reference of the given element at the cursor.
     *
     * @param elementAtCursor The PsiElement at the cursor position.
     * @return The name reference of the element.
     */
    private static String getNameReference(PsiElement elementAtCursor) {
        PsiElement cursorParent = elementAtCursor.getParent();
        String text = cursorParent.getText();
        text = text.replace("\\”","").replace("\n","");
        String[] references = text.split("\\.");
        String nameReference = references[references.length-2];
        String[] commands = nameReference.split(";");
        nameReference = commands[commands.length-1];
        if (nameReference.contains("connect(")){
            nameReference = references[references.length-3];
        }
        nameReference = nameReference.trim();
        return nameReference;
    }


    /**
     * Retrieves the contract definition PSI element from the given file.
     *
     * @param file The PSI file to search for the contract definition.
     * @return The contract definition PSI element if found, or null otherwise.
     */
    private PsiElement getContractDefinition(PsiFile file){
        for (PsiElement element : file.getChildren()){
            String nodeType = element.getNode().getElementType().toString();
            if (nodeType.equals("CONTRACT_DEFINITION")) {
                return element;
            }
        }
        return null;
    }

    //TODO What if the variable has been initialized in the class?

    /**
     * Gets the name of the reference and resolves this. It will look for something like this:
     *      const Stio = await ethers.getContractFactory("Stio");
     *      const stio = await Stio.deploy();
     * Where it looks when the ContractFactory will be called and which Smart Contract is being called there.
     * The function will give a null if it is not references to a Smart Contract.
     * @param referenceName The name of the reference
     * @Param elementAtCursor The psiElement where the cursor is currently located
     * @return The name of the SmartContract
     */
    private String resolveContractVariableReference(String referenceName, PsiElement elementAtCursor){
        resolveInitializer(referenceName,elementAtCursor);
        LOG.debug("Called resolveContractVariableReference with value " + referenceName);
        PsiElement parent = elementAtCursor.getParent();
        while (parent != null && !(parent instanceof JSBlockStatement)) {
            parent = parent.getParent();
        }
        for (PsiElement childElement : parent.getChildren()){
            if (childElement instanceof JSVarStatement varStatement) {
                JSVariable[] variables = varStatement.getVariables();
                for (JSVariable jsvarVariables: variables) {
                    if(referenceName.equals(jsvarVariables.getName())) {
                        JSExpression initializer = jsvarVariables.getInitializerOrStub();
                        // PROBLEM:
                        // const { stio, contractOwner, alice, bob } = await myCustomDeploy(); gets initializer=null but not const stio = await getDeployedContract();
                        if (initializer == null){
                            //Initializer is null, so it has to be initialized out of current scope. We need do use jsvarVariables.getStatement() to look for the initialization

                            //Get position of variable declaration
                            String constant = jsvarVariables.getStatement().getText().trim().replace("\n","").split("=")[0];
                            int position = 0;
                            if (constant.contains("{")){
                                //We have then something like this const { stio, contractOwner, alice, bob } = await myCustomDeploy();
                                //Get everything between {} and split it by , in constant
                                String[] constants = constant.substring(constant.indexOf("{") + 1, constant.indexOf("}")).replace(" ", "").split(",");
                                //Now look for the position of the element that is equal to referenceName and save the position number
                                position = Arrays.asList(constants).indexOf(referenceName);
                            }


                            //Get function name
                            String[] statements = jsvarVariables.getStatement().getText().trim().replace("\n","").split("\\.");
                            statements = statements[statements.length-1].replace(";","").split(" ");
                            String functionName = statements[statements.length-1];
                            functionName = functionName.replaceAll("\\(.*\\)", "");
                            //Search for function with the name of functionName
                            PsiElement[] jsFunctions = jsvarVariables.getDeclarationScope().getParent().getParent().getChildren();
                            for (PsiElement potentialFunciton : jsFunctions) {
                                if ("JS:FUNCTION_DECLARATION".equals(potentialFunciton.getNode().getElementType().toString())){
                                    if(potentialFunciton instanceof JSFunction function){
                                        if (functionName.equals(function.getName())){
                                            //We are in the correct function, now we have to find how the variable was called here and find it
                                            //We have to look in the return statement first
                                            //We saved the position earlier
                                            for (PsiElement statement : function.getLastChild().getChildren()){
                                                //Look for return and get the name at saved position
                                                if (statement.getNode().getElementType().toString().equals("JS:RETURN_STATEMENT")) {
                                                    if(statement instanceof JSReturnStatement returnStatement){
                                                        PsiElement element = returnStatement.getExpression().getChildren()[position];
                                                        //Ladies and gentlemen... we got him
                                                        //Can be either ES6PropertyImpl class or JSPropertyImpl
                                                        if (element instanceof PsiNamedElement namedElement){
                                                            return resolveContractVariableReference(namedElement.getName(),namedElement);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                        //Now we are at the point where the contract is being initialied.
                        //Possible:
                        // await (await ethers.getContractFactory("Stio")).deploy()
                        PsiElement resolvedElement = null;
                        if (initializer.getText().contains(".getContractFactory(")){
                            //FIXME Extract await await ethers.getContractFactory("Stio") from (await ethers.getContractFactory("Stio")).deploy()
                            //and set resolvedElement to it
                            //resolvedElement = initializer;

                            String text = initializer.getText();
                            //Extract variable from function getContractFactory(THISVARIABLE) Keep in mind that there are other functions around it
                            String stioVariable = text.substring(text.indexOf(".getContractFactory(") + 20, text.indexOf(")")).replace("\"", "");
                            return stioVariable;
                        }else {
                            // await Stio.deploy()
                            if (initializer instanceof JSPrefixExpression prefixExpression) {
                                JSExpression methodExpression = prefixExpression.getExpression();

                                if (methodExpression instanceof JSCallExpression callExpression) {
                                    JSExpression expression = callExpression.getMethodExpression();

                                    if (expression instanceof JSReferenceExpression referenceExpression) {
                                        PsiElement firstChild = referenceExpression.getFirstChild();

                                        if (firstChild instanceof JSReferenceExpression) {
                                            resolvedElement = ((JSReferenceExpression) firstChild).resolve();

                                            if (resolvedElement instanceof JSVariable jsVariable){
                                                resolvedElement = jsVariable.getInitializer();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (resolvedElement == null){
                            LOG.debug("Resolved element was null");
                            return null;
                        }
                        //Now we have the psiElement with the .getContractFactory
                        if (resolvedElement instanceof JSPrefixExpression prefixExpression) {
                            JSExpression awaitedExpression = prefixExpression.getExpression();

                            if (awaitedExpression instanceof JSCallExpression callExpression) {
                                JSArgumentList argumentList = callExpression.getArgumentList();

                                if (argumentList != null && argumentList.getArguments().length > 0) {
                                    JSExpression firstArgument = argumentList.getArguments()[0];

                                    // Handle argument if it's a reference
                                    if (firstArgument instanceof JSReferenceExpression argumentReference) {
                                        PsiElement resolvedReference = argumentReference.resolve();

                                        if (resolvedReference instanceof JSVariable resolvedVariable) {
                                            JSExpression variableInitializer = resolvedVariable.getInitializer();

                                            if (variableInitializer != null) {
                                                return variableInitializer.getText().replace("\"","");
                                            }
                                        }
                                    }else {
                                        return firstArgument.getText().replace("\"","");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * This function gets the name of a reference and the current psi element where the reference has been found. It traces back the references until it finds a function which is called getContractFactory and throws the PSIElement of that.
     *
     * @param referenceName      The name of the reference.
     * @param currentPsiElement  The current PSI element where the reference is found.
     * @return The PSIElement of the initializer function with the name "getContractFactory",
     *                           or null if the initializer function is not found.
     */
    private PsiElement resolveInitializer(String referenceName,PsiElement currentPsiElement){
        //Move to the next block
        PsiElement parent = currentPsiElement.getParent();
        while (parent != null && !(parent instanceof JSBlockStatement)) {
            parent = parent.getParent();
        }
        //Search for element there
        for (PsiElement childElement : parent.getChildren()){
            //We are looking for variables which have been initialized
            if (childElement instanceof JSVarStatement varStatement) {
                JSVariable[] variables = varStatement.getVariables();
                //We iterate through each variable to find the one we are looking for
                for (JSVariable jsvarVariables: variables) {
                    if(referenceName.equals(jsvarVariables.getName())) {
                        //At this point we have found the variable we are looking for
                        //We now look how this one got initialized
                        JSExpression initializer = jsvarVariables.getInitializer();
                        int position = 0;
                        if (initializer == null){
                            //If it was null we get something like this:
                            //const { stio, contractOwner, alice, bob } = await myCustomDeploy();
                            //we have to resolve this first

                            //We look first on which position our name we look for is
                            String constant = jsvarVariables.getStatement().getText().trim().replace("\n","").split("=")[0];
                            String[] constants = constant.substring(constant.indexOf("{") + 1, constant.indexOf("}")).replace(" ", "").split(",");
                            //Now look for the position of the element that is equal to referenceName and save the position number
                            position = Arrays.asList(constants).indexOf(referenceName);
                        }
                        //If it was not null we get something like this:
                        //await Stio.deploy()
                        //await getDeployedContract()
                        //Get function name
                        String[] statements = jsvarVariables.getStatement().getText().trim().replace("\n","").split("\\.");
                        statements = statements[statements.length-1].replace(";","").split(" ");
                        String functionName = statements[statements.length-1];
                        functionName = functionName.replaceAll("\\(.*\\)", "");
                        //We either need to resolve the function first or we can directly look if it points to the factory
                        System.out.println(functionName);
                        JSFunction function = searchFunctionInFile(functionName,currentPsiElement);

                    }
                }
            }
        }
        return null;
    }

    /**
     * Searches for a function with the given name in the parent file scope.
     *
     * @param functionName      The name of the function to search for.
     * @param currentPsiElement The current PSI element.
     * @return The PSIElement of the function with the given name, or null if not found.
     */
    private JSFunction searchFunctionInFile(String functionName, PsiElement currentPsiElement){
        PsiElement fileScope = currentPsiElement.getParent();
        while (fileScope != null && !"FILE".equals(fileScope.getNode().getElementType().toString())) {
            fileScope = fileScope.getParent();
        }
        PsiElement[] jsFunctions = fileScope.getChildren();
        for (PsiElement potentialFunciton : jsFunctions) {
            if(potentialFunciton instanceof JSFunction function){
                if (functionName.equals(function.getName())){
                    return function;
                }
            }
        }
        return null;
    }
}
