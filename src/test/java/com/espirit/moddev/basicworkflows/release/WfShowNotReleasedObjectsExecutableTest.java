package com.espirit.moddev.basicworkflows.release;

import com.espirit.moddev.basicworkflows.util.BaseContextRule;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.UIAgent;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WfShowNotReleasedObjectsExecutableTest {

    @Rule
    public BaseContextRule contextRule = new BaseContextRule(Locale.GERMAN);

    private WfShowNotReleasedObjectsExecutable testling;
    private HashMap<String, Object> arguments;
    private String dialogMessage;

    @Mock
    private WorkflowScriptContext context;

    @Mock
    private UIAgent uiAgent;

    @Mock
    private Language language;
    private java.util.Map<Object, Object> sessionMap;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Locale.setDefault(Locale.GERMANY);
    }

    @Before
    public void setUp() throws Exception {
        testling = new WfShowNotReleasedObjectsExecutable() {
            @Override
            protected void showDialog(final WorkflowScriptContext workflowScriptContext, final String title, final String message) {
                dialogMessage = message;
            }
        };

        sessionMap = new HashMap<Object, Object>();

        when(context.getSession()).thenReturn(sessionMap);
        when(context.requireSpecialist(UIAgent.TYPE)).thenReturn(uiAgent);
        when(uiAgent.getDisplayLanguage()).thenReturn(language);
        when(language.getLocale()).thenReturn(Locale.GERMANY);

        arguments = new HashMap<String, Object>();
        arguments.put(WorkflowConstants.CONTEXT, context);
    }

    @Test
    public void testExecuteEmptyMessage() throws Exception {
        testling.execute(arguments);

        assertThat("Expect empty string", dialogMessage, is(""));
    }

    @Test
    public void testExecuteNotReleasedMessage() throws Exception {
        Map<String, IDProvider.UidType> notReleasedElements = new HashMap<String, IDProvider.UidType>();
        notReleasedElements.put("meineUID", IDProvider.UidType.SITESTORE_LEAF);
        sessionMap.put(WorkflowConstants.WF_NOT_RELEASED_ELEMENTS, notReleasedElements);

        testling.execute(arguments);

        assertThat(dialogMessage, dialogMessage, is("Bitte geben Sie die folgenden Objekte frei:\n\nmeineUID\n"));
    }

    @Test
    public void testExecuteBrokenReferencesMessage() throws Exception {
        sessionMap.put(WorkflowConstants.WF_BROKEN_REFERENCES, Boolean.TRUE);

        testling.execute(arguments);

        assertThat(dialogMessage, dialogMessage, is("Es existieren kaputte Referenzen. Bitte beheben Sie diese."));
    }

    @Test
    public void testExecuteBothMessages() throws Exception {
        Map<String, IDProvider.UidType> brokenReferences = new HashMap<String, IDProvider.UidType>();
        brokenReferences.put("meineUID", IDProvider.UidType.SITESTORE_LEAF);
        sessionMap.put(WorkflowConstants.WF_BROKEN_REFERENCES, Boolean.TRUE);
        sessionMap.put(WorkflowConstants.WF_NOT_RELEASED_ELEMENTS, brokenReferences);

        testling.execute(arguments);

        assertThat(dialogMessage, dialogMessage, is("Bitte geben Sie die folgenden Objekte frei:\n\nmeineUID\n\n\n"
                                                    + "Es existieren kaputte Referenzen. Bitte beheben Sie diese."));
    }
}
