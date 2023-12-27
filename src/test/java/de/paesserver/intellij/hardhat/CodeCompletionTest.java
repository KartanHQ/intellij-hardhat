package de.paesserver.intellij.hardhat;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CodeCompletionTest extends LightPlatformCodeInsightFixture4TestCase {

    @Override
    protected String getTestDataPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath() + "/src/test/resources";
    }

    @Test
    public void test() {
        myFixture.configureByFile("scripts/Test1.js");
        //Jump to the part where the completion should be evoked
        int caretOffset = 214;
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        myFixture.type("stio.");
        LookupElement[] items = myFixture.completeBasic();
        List<String> methods = new ArrayList<>();
        methods.add("registerAddress");
        methods.add("deposit");
        methods.add("withdraw");

        for (LookupElement element : items){
            methods.remove(element.getLookupString());
        }

        Assert.assertTrue("expected all methods to be found",methods.isEmpty());
    }
}
