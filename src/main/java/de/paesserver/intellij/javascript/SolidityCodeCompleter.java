package de.paesserver.intellij.javascript;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

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
        String nameReference = elementAtCursor.getParent().getText().split("\\.")[0];

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

        //Going through the tree recursive
        //TODO Do not go through the tree recursive to save going through many entries which aren't interesting to us
        psiFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                super.visitElement(element);
                // Check if the element is a method
                if (element instanceof PsiNamedElement namedElement) {
                    ASTNode node = namedElement.getNode();
                    String name = namedElement.getName();
                    if (name != null && elementAtCursor.getParent() != null){

                        //TODO Look if it needs more functions
                        switch (node.getElementType().toString()){
                            case "CONTRACT_DEFINITION":
                                if(elementAtCursor.getParent().getNode().getElementType().toString().equals("BLOCK_STATEMENT"))
                                    resultSet.addElement(
                                            LookupElementBuilder.create(name)
                                                    .withIcon(PlatformIcons.CLASS_ICON)
                                                    .withTypeText(psiFile.getName(),true)
                                    );
                                break;
                            case "FUNCTION_DEFINITION":
                                if(elementAtCursor.getParent().getNode().getElementType().toString().equals("JS:REFERENCE_EXPRESSION")){

                                    registerIfReferenced(namedElement, nameReference, name, resultSet, "FUNCTION_DEFINITION");
                                }

                                break;
                            case "STATE_VARIABLE_DECLARATION":
                                if(elementAtCursor.getParent().getNode().getElementType().toString().equals("JS:REFERENCE_EXPRESSION")){
                                    String nameReference = elementAtCursor.getParent().getText().split("\\.")[0];

                                    registerIfReferenced(namedElement, nameReference, name, resultSet, "STATE_VARIABLE_DECLARATION");
                                }

                                break;
                            default:
                            //System.out.println(name);
                            //System.out.println(node.getElementType());
                            //System.out.println("---------");
                                break;
                        }
                    }
                }
            }
        });

        // Call super method to ensure other completion contributors are also invoked
        super.fillCompletionVariants(parameters, resultSet);
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
        if (method.split("\"").length > 0) {
            //Directly initialized
            if (method.split("\"").length > 1){
                return method.split("\"")[1];
            }else {
                return null;
            }

        }else {
            int firstIndex = method.indexOf("(");
            int secondIndex = method.indexOf(")");
            String name = method.substring(firstIndex,secondIndex);
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
        //TODO: Check if it is outside of block defined
        // TODO: Implement
        return null;
    }

    /**
     * Registers the named element as a completion result if it is referenced in the given context.
     *
     * @param namedElement   The named element to register
     * @param nameReference  The name of the reference
     * @param name           The name of the named element
     * @param resultSet      The completion result set to add the named element to
     * @param type           The type of the named element
     */
    private void registerIfReferenced(PsiNamedElement namedElement, String nameReference, String name, @NotNull CompletionResultSet resultSet, String type) {
        if (nameReference != null){
            //Check to which contract it references
            //TODO Check access modifier
            if (namedElement.getParent().getNode().getElementType().toString().equals("CONTRACT_DEFINITION")){
                String text = namedElement.getParent().getText();
                int index = text.indexOf("{");
                String header = (index != -1) ? text.substring(0, index).trim() : text.trim();
                String[] splitHeader = header.split(" ");
                for (int i = 0; i  < splitHeader.length;i++){
                    switch (splitHeader[i]){
                        case "is":
                            //If the reference name has the same name as the contract, we do not want its methods
                            if (splitHeader[i+1].toLowerCase().equals(nameReference)){
                                continue;
                            }
                        case "contract" :
                            if (i+1 < splitHeader.length){
                                //TODO Check access modifier

                                //TODO Even Complete Code if only a part of the method has been written
                                //E.g. myContract.withd... -> withdraw()

                                //TODO Do complete completion, not just the method name
                                //E.g   myContract. -> myContract.deposit
                                //      myContract. -> myContract.deposit()
                                
                                //Check if our current reference name equals to the contract name
                                if (splitHeader[i+1].toLowerCase().equals(nameReference)){
                                    LookupElementBuilder builder = LookupElementBuilder.create(name);
                                    switch (type){
                                        case "STATE_VARIABLE_DECLARATION":
                                            builder = builder.withIcon(PlatformIcons.VARIABLE_ICON);
                                            builder = builder.withTypeText(namedElement.getFirstChild().getText(),true);
                                            break;
                                        case "FUNCTION_DEFINITION":
                                            builder = builder.withIcon(PlatformIcons.METHOD_ICON);
                                            for(PsiElement child : namedElement.getChildren()){
                                                switch (child.getNode().getElementType().toString()) {
                                                    case "PARAMETER_LIST":
                                                        builder = builder.appendTailText(child.getText(),true);
                                                        break;
                                                    case "FUNCTION_VISIBILITY_SPECIFIER":
                                                        builder = builder.withTypeText(child.getText(),true);
                                                        break;
                                                }
                                            }
                                            break;
                                    }

                                    resultSet.addElement(builder);
                                }
                            }
                            break;
                    }
                }
            }
        }
    }
}
