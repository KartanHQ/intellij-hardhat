package de.paesserver.intellij.javascript;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

import java.sql.Array;
import java.util.Arrays;

//TODO stio.accountMap.accountMap shouldn't be possible
public class SolidityCodeCompleter extends CompletionContributor {

    private final Logger LOG = Logger.getInstance(this.getClass());

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
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
        PsiElement cursorParent = elementAtCursor.getParent();
        String nameReference = cursorParent.getText().split("\\.")[0];

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

        // Call super method to ensure other completion contributors are also invoked
        super.fillCompletionVariants(parameters, resultSet);
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
        LOG.debug("Called resolveContractVariableReference with value " + referenceName);
        //Name reference is the reference name of the object. Like myObject.anyMethod(). myObject is the reference name
        //Is needed to know which methods should be completed

        //Now look where the nameReference is located. We are looking for something like const name =...
        //Entity is probably defined in one of the children of the parent
        //TODO: Check if it is outside of parent defined
        PsiElement parentElement = elementAtCursor.getParent().getParent().getParent();
        LOG.debug("Number of child objects: " + parentElement.getChildren().length);
        for (PsiElement childElement : parentElement.getChildren()){
            if (childElement.getNode().getElementType().toString().equals("JS:VAR_STATEMENT")) {
                //Identifier found. Check if it is that, what we are looking for
                String line = childElement.getText();
                LOG.debug("Identifier found: " + line);
                long count = Arrays.stream(line.split(" ")).map(String::trim).filter(statement -> statement.equals(referenceName)).count();

                if (count > 0){
                    LOG.debug("Found reference name! -> Looking for getContractFactory");
                    //If more than 1 is found, then the deployment of the contract is here
                    String[] nameArray = Arrays.stream(line.split(" ")).map(String::trim).filter(statement -> statement.contains("deploy")).flatMap(statement -> Arrays.stream(statement.split("\\."))).toArray(String[]::new);
                    //We either find reference.deploy() or something like reference.getContractFactory.deploy()
                    //If we already have getContractFactory, we have our contract name:
                    if (Arrays.stream(nameArray).anyMatch(a -> a.contains("getContractFactory"))) {
                        LOG.debug("Found getContractFactory! -> Directly extract name out of it");
                        String method = Arrays.stream(nameArray).filter(a -> a.contains("getContractFactory")).findFirst().get();
                        return extractContractNameFromGetContractFactory(method,parentElement);
                    }else {
                        LOG.debug("Didn't found getContractFactory! -> Look for deploy");
                        if (Arrays.stream(nameArray).anyMatch(a -> a.contains("deploy("))) {
                            LOG.debug("Deploy found! -> Look for reference from reference");
                            //Deploy only found -> resolve the reference for it again
                            for (int i = 1;i < nameArray.length;i++){
                                if (nameArray[i].contains("deploy(")){
                                    LOG.debug("Found reference name: " + nameArray[i-1] + " -> calling resolveReferenceToContractFactory");
                                    return resolveReferenceToContractFactory(nameArray[i-1],parentElement);
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
     * Resolve reference to the variable which evokes getContractFactory
     * @param referenceName Name of the reference to look for
     * @param block Block, in which it looks
     * @return Returns the name of the contract if found, or else null
     */
    private String resolveReferenceToContractFactory(String referenceName, PsiElement block){
        //TODO: Check if it is outside of block defined
        for (PsiElement childElement : block.getChildren()){
            if (childElement.getNode().getElementType().toString().equals("JS:VAR_STATEMENT")) {
                String line = childElement.getText();
                long count = Arrays.stream(line.split(" ")).map(String::trim).filter(statement -> statement.equals(referenceName)).count();
                if (count > 0) {
                    //If more than 1 is found, then the declaration of the contract is here
                    //Now our getContractFactory has to be here
                    String method = Arrays.stream(line.split(" ")).map(String::trim).filter(statement -> statement.contains("getContractFactory")).flatMap(statement -> Arrays.stream(statement.split("\\."))).filter(a -> a.contains("getContractFactory")).findFirst().orElse(null);
                    if (method != null){
                        return extractContractNameFromGetContractFactory(method,block);
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param method The string of the called method from ContractFactory like getContractFactory("Stoi")
     * @param block Block where to look for the reference name (needed for calling resolveVariableReference9
     * @return The initialized name or null if not found
     */
    private String extractContractNameFromGetContractFactory(String method, PsiElement block){
        //TODO: Check if it is outside of block defined
        //The name is either written in the header or in a variable
        if (method.split("\"").length > 2) {
            //Directly initialized
            return method.split("\"")[1];

        }else {
            int firstIndex = method.indexOf("(");
            int secondIndex = method.indexOf(")");
            String name = method.substring(firstIndex+1,secondIndex);
            return resolveMethodVariableReference(name,block);
        }
    }



    /**
     *  Resolves what the variable name contains if it has been directly initialized
     * @param referenceName Name of the variable reference
     * @param block Block where to look for the reference name
     * @return The initialized name or null if not found
     */
    private String resolveMethodVariableReference(String referenceName, PsiElement block){
        for (PsiElement childElement : block.getChildren()){
            if (childElement instanceof JSVarStatement varStatement) {
                JSVariable[] variables = varStatement.getVariables();
                for (JSVariable variable: variables) {
                    if(variable.getName().equals(referenceName)) {
                        JSExpression initializer = variable.getInitializerOrStub();
                        return initializer.getText();
                    }
                }
            }
        }
        return null;
    }
}
