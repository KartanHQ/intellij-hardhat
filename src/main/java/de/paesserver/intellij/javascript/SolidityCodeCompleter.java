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

public class SolidityCodeCompleter extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        // Get the project and file manager
        Project project = parameters.getPosition().getProject();
        PsiManager psiManager = PsiManager.getInstance(project);
        Editor editor = parameters.getEditor();
        PsiFile currentFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        int offset = editor.getCaretModel().getOffset();

        // Use PSIUtil to get the PSI element at the cursor offset
        PsiElement elementAtCursor = currentFile.findElementAt(offset);

        // Retrieve the virtual file corresponding to the Solidity contracts directory
        VirtualFile solidityDir = project.getBaseDir().findFileByRelativePath("contracts");
        if (solidityDir != null && solidityDir.isDirectory()) {
            // Iterate through files in the Solidity directory
            for (VirtualFile file : solidityDir.getChildren()) {
                // Ensure it's a Solidity file before processing
                if ("sol".equalsIgnoreCase(file.getExtension())) {
                    PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile != null && psiFile.toString().equals("Solidity File")){
                        psiFile.accept(new PsiRecursiveElementVisitor() {
                            @Override
                            public void visitElement(@NotNull PsiElement element) {
                                super.visitElement(element);
                                // Check if the element is a method
                                if (element instanceof PsiNamedElement namedElement) {
                                    ASTNode node = namedElement.getNode();
                                    String name = namedElement.getName();
                                    if (name != null && elementAtCursor != null && elementAtCursor.getParent() != null){

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
                                                    //Name reference is the reference name of the object. Like myObject.anyMethod(). myObject is the reference name
                                                    //Is needed to know which methods should be completed
                                                    String nameReference = elementAtCursor.getParent().getText().split("\\.")[0];

                                                    registerIfReferenced(namedElement, nameReference, name, resultSet, "FUNCTION_DEFINITION");
                                                }

                                                break;
                                            case "STATE_VARIABLE_DECLARATION":
                                                if(elementAtCursor.getParent().getNode().getElementType().toString().equals("JS:REFERENCE_EXPRESSION")){
                                                    String nameReference = elementAtCursor.getParent().getText().split("\\.")[0];

                                                    registerIfReferenced(namedElement, nameReference, name, resultSet, "STATE_VARIABLE_DECLARATION");
                                                }

                                                break;
                                            default: break;
                                                //System.out.println(name);
                                                //System.out.println(node.getElementType());
                                                //System.out.println("---------");
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }

        // Call super method to ensure other completion contributors are also invoked
        super.fillCompletionVariants(parameters, resultSet);
    }

    private static void registerIfReferenced(PsiNamedElement namedElement, String nameReference, String name, @NotNull CompletionResultSet resultSet, String type) {
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
                        case "contract" :
                            if (i+1 < splitHeader.length){
                                //TODO contract [name] is [otherContract] should be considered

                                //TODO Check access modifier

                                //TODO Even Complete Code if only a part of the method has been written
                                //E.g. myContract.withd... -> withdraw()

                                //TODO Do complete completion, not just the method name
                                //E.g   myContract. -> myContract.deposit
                                //      myContract. -> myContract.deposit()

                                //TODO Follow back reference to get actual contract name so this works:
                                // const Stio = await ethers.getContractFactory("Stio");
                                // const deployedContract = await Stio.deploy();
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
