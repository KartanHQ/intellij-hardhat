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

import java.util.ArrayList;
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
                LOG.debug("Couldn't find cursor");
            }
            return;
        }
        //Name reference is the reference name of the object. Like myObject.anyMethod(). myObject is the reference name
        //Is needed to know which methods should be completed
        //It should return if the cursor is behind anyMethod. and therefore not referring to a contract
        String nameReference = getNameReference(elementAtCursor);
        if (nameReference == null){
            //No name found -> no completion
            LOG.debug("Couldn't find name reference");
            return;
        }

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
        if (references.length > 1){
            String nameReference = references[references.length-2];
            String[] commands = nameReference.split(";");
            nameReference = commands[commands.length-1];
            if (nameReference.contains("connect(")){
                nameReference = references[references.length-3];
            }
            nameReference = nameReference.trim();
            return nameReference;
        }
        return null;
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
        PsiElement resolved = resolveInitializer(referenceName,elementAtCursor);
        if (resolved == null){
            return null;
        }
        String text = resolved.getText();
        //Extract variable from function getContractFactory(THISVARIABLE) Keep in mind that there are other functions around it
        String stioVariable = text.substring(text.indexOf(".getContractFactory(") + 20, text.indexOf(")"));
        //If there are no " then it is a variable and we have to resolve that
        if (!stioVariable.contains("\"")){
            if (resolved instanceof JSPrefixExpression prefixExpression) {
                resolved = prefixExpression.getExpression();
            }
            if (resolved instanceof  JSCallExpression callExpression){
                JSArgumentList argumentList = callExpression.getArgumentList();

                if (argumentList != null && argumentList.getArguments().length > 0) {
                    JSExpression firstArgument = argumentList.getArguments()[0];

                    // Handle argument if it's a reference
                    if (firstArgument instanceof JSReferenceExpression argumentReference) {
                        PsiElement resolvedReference = argumentReference.resolve();

                        if (resolvedReference instanceof JSVariable resolvedVariable) {
                            JSExpression variableInitializer = resolvedVariable.getInitializer();

                            if (variableInitializer != null) {
                                stioVariable = variableInitializer.getText().replace("\"","");
                            }
                        }
                    }else {
                        stioVariable = firstArgument.getText().replace("\"","");
                    }
                }
            }
        }else {
            stioVariable = stioVariable.replace("\"","");
        }
        return stioVariable;
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
        if (parent == null){
            return null;
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
                        JSCallExpression deployExpression = null;
                        if (initializer == null){
                            //If it was null we get something like this:
                            //const { stio, contractOwner, alice, bob } = await myCustomDeploy();
                            //const { stio, contractOwner, alice, bob } = await loadFixture(myCustomDeploy);
                            //we have to resolve this first

                            //We look first on which position our name we look for is
                            String constant = jsvarVariables.getStatement().getText().trim().replace("\n","").split("=")[0];
                            String[] constants = constant.substring(constant.indexOf("{") + 1, constant.indexOf("}")).replace(" ", "").split(",");
                            //Now look for the position of the element that is equal to referenceName and save the position number
                            position = Arrays.asList(constants).indexOf(referenceName);

                            //Get function name
                            String[] statements = jsvarVariables.getStatement().getText().trim().replace("\n","").split("\\.");
                            statements = statements[statements.length-1].replace(";","").split(" ");
                            //FIXME There is also a better way to do this
                            String functionName = statements[statements.length-1];
                            if (functionName.contains("loadFixture(")){
                                functionName = functionName.substring(functionName.indexOf("(")+1,functionName.indexOf(")"));
                            }
                            functionName = functionName.replaceAll("\\(.*\\)", "");
                            deployExpression = resolveUnknownFunctionUntilDeploy(functionName,currentPsiElement,position);
                        }else {
                            //If it was not null we get something like this:
                            //await Stio.deploy()
                            //await getDeployedContract()
                            //We either need to resolve the function first or we can directly look if it points to the factory
                            //So we need to look if there is a deploy or factory. If not we have to resolve the functions to that
                            if (initializer instanceof JSPrefixExpression prefixExpression){
                                initializer = prefixExpression.getExpression();
                            }
                            if (initializer instanceof JSCallExpression callExpression){
                                String[] calls = callExpression.getMethodExpression().getText().split("\\.");
                                String functionName = calls[calls.length-1];
                                if (functionName.equals("deploy")){
                                    deployExpression = callExpression;
                                } else
                                    if (functionName.equals("getContractFactory")){
                                        return initializer;
                                    }else{
                                        //If both aren't there we have an unknown function
                                        deployExpression = resolveUnknownFunctionUntilDeploy(functionName,currentPsiElement);
                                    }
                            }
                        }
                        //At this point we have the deploy or getfactory in our scope and we have the reference we have to look for
                        return resolveDeployToContractFactory(deployExpression);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolves the contract factory reference in the deploy expression.
     *
     * @param deployExpression The deploy expression representing the contract deployment.
     * @return The PSI element of the resolved contract factory reference.
     */
    private PsiElement resolveDeployToContractFactory(@NotNull JSCallExpression deployExpression){
        PsiElement referenceObject = deployExpression.getMethodExpression().getFirstChild();
        if (referenceObject instanceof JSReferenceExpression referenceExpression){
            referenceObject = referenceExpression.resolve();
        }
        if (referenceObject instanceof JSVariable jsVariable){
            referenceObject = jsVariable.getInitializer();
        }
        if (referenceObject instanceof JSPrefixExpression prefixExpression){
            referenceObject = prefixExpression.getExpression();
        }
        return referenceObject;
    }
    private JSCallExpression resolveUnknownFunctionUntilDeploy(String functionName, PsiElement currentPsiElement){
        return resolveUnknownFunctionUntilDeploy(functionName,currentPsiElement,0);
    }
    /**
     * Resolves an unknown function until the deploy statement is reached. This method recursively searches for the given function name in the parent file scope until it finds the
     * deploy statement or reaches the top-level file scope.
     *
     * @param functionName       The name of the function to resolve.
     * @param currentPsiElement  The current PSI element being searched in.
     * @param returnPosition     The index of the return element to resolve (if the return element is a function call).
     * @return The resolved JSCallExpression representing the deploy statement, or null if the function or deploy statement is not found.
     */
    private JSCallExpression resolveUnknownFunctionUntilDeploy(@NotNull String functionName,@NotNull PsiElement currentPsiElement,int returnPosition){
        //TODO Recursive until Deploy
        JSFunction function = searchFunctionInParent(functionName,currentPsiElement);
        if (function == null){
            //Couldn't find function Maybe in another class/file?
            //TODO look if function is elsewhere defined
            //Probably doing something like that recursively would make sens
            return null;
        }
        //Now we have to look for the reference name we are looking for
        //We need to look at the return statement
        PsiElement returnElement = getReturnReferenceAtPosition(returnPosition,function);
        //We either have a variable name there of a function

        //Variable
        if (returnElement instanceof JSProperty property){
            if (property.getInitializer() instanceof JSReferenceExpression referenceExpression){
                PsiElement statement = referenceExpression.resolve();
                if (statement instanceof JSVariable variable){
                    PsiElement initializer = variable.getInitializer();
                    if (initializer instanceof JSPrefixExpression prefixExpression){
                        initializer = prefixExpression.getExpression();
                    }
                    if (initializer instanceof JSCallExpression callExpression){
                        //Just set returnElement as call Expression, so it can be handled below
                        returnElement = callExpression;
                    }
                }
            }
        }

        //Function
        if (returnElement instanceof JSCallExpression callExpression){
            if (callExpression.getText().contains(".deploy()")){
                return callExpression;
            }else {
                //Look for the function if there is another custom function
                //FIXME Bad way to look for it but better than nothing
                String[] methodStrings = callExpression.getText().replace("(","").replace(")","").split("\\.");
                return resolveUnknownFunctionUntilDeploy(methodStrings[methodStrings.length-1],callExpression,0);
            }
        }
        return null;
    }

    private JSFunction searchFunctionInParent(@NotNull String functionName, PsiElement currentPsiElement){
        if (currentPsiElement == null || currentPsiElement.getParent() == null){
            return null;
        }
        PsiElement parent = currentPsiElement.getParent();

        PsiElement[] jsFunctions = parent.getChildren();
        for (PsiElement potentialFunction : jsFunctions) {
            if(potentialFunction instanceof JSFunction function){
                if (functionName.equals(function.getName())){
                    return function;
                }else {
                    //Look if it might be defined in the function
                    JSFunction inFunction = searchInFunction(functionName,function);
                    if (inFunction != null){
                        return inFunction;
                    }
                }
            }
        }
        return searchFunctionInParent(functionName,parent);
    }

    private JSFunction searchInFunction(@NotNull String functionName,@NotNull JSFunction function){
        PsiElement[] childs = function.getLastChild().getChildren();
        for (PsiElement child : childs){
            if(child instanceof JSFunction subFunction){
                if (functionName.equals(subFunction.getName())){
                    return function;
                }
            }
        }
        return null;
    }

    private PsiElement getReturnReferenceAtPosition(int position,@NotNull JSFunction function){
        for (PsiElement statement : function.getLastChild().getChildren()){
            //Look for return and get the name at saved position
            if (statement.getNode().getElementType().toString().equals("JS:RETURN_STATEMENT")) {
                if(statement instanceof JSReturnStatement returnStatement){
                    //Ladies and gentlemen... we got him
                    //Can be either ES6PropertyImpl class or JSPropertyImpl
                    PsiElement[] elements = returnStatement.getExpression().getChildren();
                    ArrayList<PsiElement> filteredElements = new ArrayList<>();
                    for (PsiElement element : elements){
                        //Remove whitespace, awaits
                        switch (element.getNode().getElementType().toString()){
                            case "JS:AWAIT_KEYWORD":
                            case "WHITE_SPACE":break;
                            default: filteredElements.add(element);
                        }
                    }
                    return filteredElements.get(position);
                }
            }
        }
        return null;
    }
}
