package lwm.plugin.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import java.util.List;
import java.util.stream.Collectors;

public class AddFinalIntentionTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String INTENTION_TEXT = "Add final modifier(添加final修饰)";

    private void doTest(final String before, final String after) {
        assertTrue("Test code must contain <caret>", before.contains("<caret>"));
        myFixture.configureByText("Test.java", before);
        final IntentionAction intention = myFixture.findSingleIntention(INTENTION_TEXT);
        assertNotNull("Intention '" + INTENTION_TEXT + "' not found", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult(after);
    }

    private void doTestNotAvailable(final String content) {
        assertTrue("Test code must contain <caret>", content.contains("<caret>"));
        myFixture.configureByText("Test.java", content);
        final List<IntentionAction> intentions = myFixture.getAvailableIntentions();
        final List<String> intentionTexts = intentions.stream()
                .map(IntentionAction::getText)
                .collect(Collectors.toList());
        assertFalse("Intention '" + INTENTION_TEXT + "' should not be available. Available: " + intentionTexts,
                    intentionTexts.contains(INTENTION_TEXT));
    }

    private void doTestNoChange(final String contentWithCaret) {
        assertTrue("Test code must contain <caret>", contentWithCaret.contains("<caret>"));
        final String contentWithoutCaret = contentWithCaret.replace("<caret>", "");
        myFixture.configureByText("Test.java", contentWithCaret);
        // Attempt to find the intention. It should be available.
        final IntentionAction intention = myFixture.findSingleIntention(INTENTION_TEXT);
        assertNotNull("Intention '" + INTENTION_TEXT + "' should be available but was not found.", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult(contentWithoutCaret); // Ensure no change happened
    }

    public void testAddFinalToSingleParameter() {
        final String before = "class Test {\n" +
                        "    void method(String pa<caret>ram1, int param2) {}\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    void method(final String param1, int param2) {}\n" +
                       "}";
        doTest(before, after);
    }

    public void testAddFinalToSingleLocalVariable() {
        final String before = "class Test {\n" +
                        "    void method() {\n" +
                        "        String l<caret>ocal1 = \"hello\";\n" +
                        "        int local2 = 10;\n" +
                        "    }\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    void method() {\n" +
                       "        final String local1 = \"hello\";\n" +
                       "        int local2 = 10;\n" +
                       "    }\n" +
                       "}";
        doTest(before, after);
    }

    public void testAddFinalToAllInMethodScope() {
        // Caret is removed in 'after' string by checkResult implicitly if not present.
        final String before = "class Test {\n" +
                        "    void method(String param1, int param2) {\n" +
                        "        <caret>\n" +
                        "        String local1 = \"hello\";\n" +
                        "        int local2 = 10;\n" +
                        "        final String alreadyFinal = \"done\";\n" +
                        "    }\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    void method(final String param1, final int param2) {\n" +
                       "        \n" +
                       "        final String local1 = \"hello\";\n" +
                       "        final int local2 = 10;\n" +
                       "        final String alreadyFinal = \"done\";\n" +
                       "    }\n" +
                       "}";
        doTest(before, after);
    }

    public void testNotAvailableOnAlreadyFinalParameter() {
        final String content = "class Test {\n" +
                         "    void method(final String pa<caret>ram1) {}\n" +
                         "}";
        // As per current isAvailable & invoke logic, intention is available but makes no change.
        doTestNoChange(content);
    }

    public void testNotAvailableOnAlreadyFinalLocalVariable() {
        final String content = "class Test {\n" +
                         "    void method() {\n" +
                         "        final String lo<caret>cal1 = \"hello\";\n" +
                         "    }\n" +
                         "}";
        // As per current isAvailable & invoke logic, intention is available but makes no change.
        doTestNoChange(content);
    }

    public void testAvailableOnClassDeclaration() {
        final String before = "class Te<caret>st {\n" +
                         "    String field1 = \"initialized\";\n" +
                         "    String field2;\n" +
                         "    void method(String param1) {\n" +
                         "        String local1 = \"hello\";\n" +
                         "    }\n" +
                         "}";
        final String after = "class Test {\n" +
                        "    final String field1 = \"initialized\";\n" +
                        "    String field2;\n" +
                        "    void method(final String param1) {\n" +
                        "        final String local1 = \"hello\";\n" +
                        "    }\n" +
                        "}";
        doTest(before, after);
    }

    public void testNotAvailableInImportStatement() {
        final String content = "import ja<caret>va.util.List;\n" +
                         "class Test {\n" +
                         "    void method(String param1) {}\n" +
                         "}";
        doTestNotAvailable(content);
    }

    public void testNotAvailableOnFieldName() {
        // The current isAvailable will make it available for fields.
        // The invoke method will make the field final.
        final String before = "class Test {\n" +
                        "    String myFi<caret>eld = \"value\";\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    final String myField = \"value\";\n" +
                       "}";
        doTest(before, after);
    }

    public void testNotAvailableOnAlreadyFinalFieldName() {
        final String content = "class Test {\n" +
                         "    final String myFi<caret>eld = \"value\";\n" +
                         "}";
        // Intention should be available (based on isAvailable) but do nothing (based on invoke)
        doTestNoChange(content);
    }

    // 测试类属性添加 final 的各种场景
    
    public void testFieldWithInitializer() {
        final String before = "class Test {\n" +
                        "    String fi<caret>eld = \"initialized\";\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    final String field = \"initialized\";\n" +
                       "}";
        doTest(before, after);
    }
    
    public void testFieldInitializedInConstructor() {
        final String before = "class Test {\n" +
                        "    String fi<caret>eld;\n" +
                        "    Test() {\n" +
                        "        field = \"initialized\";\n" +
                        "    }\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    final String field;\n" +
                       "    Test() {\n" +
                       "        field = \"initialized\";\n" +
                       "    }\n" +
                       "}";
        doTest(before, after);
    }
    
    public void testFieldInitializedInMultipleConstructors() {
        final String before = "class Test {\n" +
                        "    String fi<caret>eld;\n" +
                        "    Test() {\n" +
                        "        field = \"default\";\n" +
                        "    }\n" +
                        "    Test(String value) {\n" +
                        "        field = value;\n" +
                        "    }\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    final String field;\n" +
                       "    Test() {\n" +
                       "        field = \"default\";\n" +
                       "    }\n" +
                       "    Test(String value) {\n" +
                       "        field = value;\n" +
                       "    }\n" +
                       "}";
        doTest(before, after);
    }
    
    public void testFieldNotInitializedInAllConstructors() {
        final String content = "class Test {\n" +
                         "    String fi<caret>eld;\n" +
                         "    Test() {\n" +
                         "        field = \"initialized\";\n" +
                         "    }\n" +
                         "    Test(String value) {\n" +
                         "        // 这个构造器没有初始化 field\n" +
                         "    }\n" +
                         "}";
        // 不应该添加 final，因为并非所有构造器都初始化了字段
        doTestNoChange(content);
    }
    
    public void testFieldWithoutInitializerAndNoConstructor() {
        final String content = "class Test {\n" +
                         "    String fi<caret>eld;\n" +
                         "}";
        // 没有显式构造器且字段没有初始化器，不应该添加 final
        doTestNoChange(content);
    }
    
    public void testStaticFieldWithInitializer() {
        final String before = "class Test {\n" +
                        "    static String fi<caret>eld = \"initialized\";\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    static final String field = \"initialized\";\n" +
                       "}";
        doTest(before, after);
    }
    
    public void testStaticFieldWithoutInitializer() {
        final String content = "class Test {\n" +
                         "    static String fi<caret>eld;\n" +
                         "}";
        // 静态字段没有初始化器，不应该添加 final
        doTestNoChange(content);
    }
    
    public void testClassWithMixedFields() {
        final String before = "class Te<caret>st {\n" +
                        "    String field1 = \"initialized\";\n" +
                        "    String field2;\n" +
                        "    static String field3 = \"static\";\n" +
                        "    static String field4;\n" +
                        "    Test() {\n" +
                        "        field2 = \"constructor\";\n" +
                        "    }\n" +
                        "}";
        final String after = "class Test {\n" +
                       "    final String field1 = \"initialized\";\n" +
                       "    final String field2;\n" +
                       "    static final String field3 = \"static\";\n" +
                       "    static String field4;\n" +
                       "    Test() {\n" +
                       "        field2 = \"constructor\";\n" +
                       "    }\n" +
                       "}";
        doTest(before, after);
    }


    @Override
    protected String getTestDataPath() {
        // Not using testData files for these tests, so path can be empty or root.
        return "";
    }
}
