package de.paesserver.intellij.hardhat;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeCompletionTest extends LightPlatformCodeInsightFixture4TestCase {

    @Override
    protected String getTestDataPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath() + "/src/test/resources";
    }

    @Before
    public void setup(){
        Path src = Paths.get(getTestDataPath());
        Path dest = Paths.get(Objects.requireNonNull(getProject().getBasePath()));

        File directory = new File(String.valueOf(dest));
        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null && files.length > 0){
                return;
            }
        }

        try {
            Files.walk(src)
                    .forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    public void noCompletion() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        {
            myFixture.configureByFile("scripts/Test0.js");
            //Jump to the part where the completion should be evoked
            int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
            myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

            myFixture.type("stio.");

            LookupElement[] items = myFixture.completeBasic();
            for (LookupElement element : items){
                suggestions.remove(element.getLookupString());
            }
            Assert.assertEquals("expected no methods to be suggested", 5, suggestions.size());
        }
        {
            myFixture.configureByFile("scripts/Test4.js");
            //Jump to the part where the completion should be evoked
            int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
            myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

            myFixture.type("stio.");
            LookupElement[] items = myFixture.completeBasic();
            for (LookupElement element : items){
                suggestions.remove(element.getLookupString());
            }
            Assert.assertEquals("expected no methods to be suggested", 5, suggestions.size());
        }
        {
            myFixture.configureByFile("scripts/Test1.js");
            //Jump to the part where the completion should be evoked
            int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code0");
            myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

            myFixture.type("stio.");
            LookupElement[] items = myFixture.completeBasic();
            for (LookupElement element : items){
                suggestions.remove(element.getLookupString());
            }
            Assert.assertEquals("expected no methods to be suggested", 5, suggestions.size());
        }
    }

    @Test
    public void separateReference() {
        myFixture.configureByFile("scripts/Test1.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        LookupElement[] items = myFixture.completeBasic();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        int initialSize = suggestions.size();

        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }

        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());
    }

    @Test
    public void singleLineReference() {
        myFixture.configureByFile("scripts/Test2.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        LookupElement[] items = myFixture.completeBasic();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        int initialSize = suggestions.size();

        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }

        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());
    }

    @Test
    public void connectInBetween() {
        myFixture.configureByFile("scripts/Test1.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.connect(alice).");
        LookupElement[] items = myFixture.completeBasic();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        int initialSize = suggestions.size();

        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }

        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());
    }

    @Test
    public void useAfterDeployFunction() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");
        int initialSize = suggestions.size();

        myFixture.configureByFile("scripts/Test1.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code2");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        LookupElement[] items = myFixture.completeBasic();
        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }
        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());

        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");
        initialSize = suggestions.size();

        //Jump to the part where the completion should be evoked
        caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code3");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        items = myFixture.completeBasic();
        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }
        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());

        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");
        initialSize = suggestions.size();

        //Jump to the part where the completion should be evoked
        caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code4");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        items = myFixture.completeBasic();
        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }
        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());
    }

    @Test
    public void variableInGetContractFactory() {
        myFixture.configureByFile("scripts/Test3.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        LookupElement[] items = myFixture.completeBasic();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        int initialSize = suggestions.size();

        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }

        Assert.assertTrue("methods not found: [" + suggestions.size() + "/" + initialSize + "]: " + suggestions, suggestions.isEmpty());
    }

    @Test
    public void noRepeatingSuggestion() {
        myFixture.configureByFile("scripts/Test1.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = myFixture.getEditor().getDocument().getText().indexOf("//Code1");
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.accountMap.");
        LookupElement[] items = myFixture.completeBasic();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("registerAddress");
        suggestions.add("deposit");
        suggestions.add("withdraw");
        suggestions.add("accountMap");
        suggestions.add("owner");

        for (LookupElement element : items){
            suggestions.remove(element.getLookupString());
        }

        Assert.assertFalse("methods have been suggested", suggestions.isEmpty());
    }
}
