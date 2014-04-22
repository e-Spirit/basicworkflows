package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import de.espirit.common.TypedFilter;
import de.espirit.common.util.Listable;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.Workflowable;
import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFilter;
import de.espirit.firstspirit.access.store.templatestore.*;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.UIAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author gremplewski
 */
public class WfCheckPrerequisitesExecutableTest {

	private ResourceBundle resourceBundle;
	private RequestOperation requestOperation;
	private WorkflowScriptContext workflowScriptContext;
	private TemplateFolder templateFolder;
	private Schema schema;
	private Language language;

	@Before
	public void initMocks() {
		templateFolder = mock(TemplateFolder.class);
		schema = mock(Schema.class);

		RequestOperation.Answer noAnswer = mock(RequestOperation.Answer.class);
		RequestOperation.Answer yesAnswer = mock(RequestOperation.Answer.class);
		requestOperation = mock(RequestOperation.class);
		when(requestOperation.addNo()).thenReturn(noAnswer);
		when(requestOperation.addYes()).thenReturn(yesAnswer);

		language = mock(Language.class);
		when(language.getLocale()).thenReturn(Locale.GERMANY);

		resourceBundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, language.getLocale());

		UIAgent uiAgent = mock(UIAgent.class);
		when(uiAgent.getDisplayLanguage()).thenReturn(language);

		Project project = mock(Project.class);
		when(project.getLanguage(Locale.GERMANY.getLanguage().toUpperCase())).thenReturn(language);

		Task task = mock(Task.class);
		when(task.getCustomAttributes()).thenReturn(new HashMap<Object, Object>());

		OperationAgent operationAgent = mock(OperationAgent.class);
		when(operationAgent.getOperation(RequestOperation.TYPE)).thenReturn(requestOperation);

		workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.requireSpecialist(UIAgent.TYPE)).thenReturn(uiAgent);
		when(workflowScriptContext.getProject()).thenReturn(project);
		when(workflowScriptContext.getTask()).thenReturn(task);

		when(workflowScriptContext.requireSpecialist(OperationAgent.TYPE)).thenReturn(operationAgent);
	}

	@Test
	public void testDeleteTemplateFolderWithChildTemplate() throws IllegalAccessException {
		setMockStoreElement(templateFolder);
		Template childTemplate = mock(Template.class);
		testDeleteTemplateFolderWithChild(childTemplate);
		verify(requestOperation).perform(resourceBundle.getString("hasChildren"));
		verify(workflowScriptContext).doTransition("trigger_folder_ok");
	}

	@Test
	public void testDeleteTemplateFolderWithChildTemplateFolder() throws IllegalAccessException {
		setMockStoreElement(templateFolder);
		TemplateFolder childTemplateFolder = mock(TemplateFolder.class);
		testDeleteTemplateFolderWithChild(childTemplateFolder);
		verify(requestOperation).perform(resourceBundle.getString("hasChildren"));
		verify(workflowScriptContext).doTransition("trigger_folder_ok");
	}

	private void setMockStoreElement(StoreElement mockStoreElement) {
		when(workflowScriptContext.getStoreElement()).thenReturn(mockStoreElement);
	}

	private void testDeleteTemplateFolderWithChild(final StoreElement templateFolderChild) throws IllegalAccessException {
		when(templateFolder.getChildren(any(StoreElementFilter.class), eq(true))).thenAnswer(new Answer<Listable<StoreElement>>() {
			@Override
			public Listable<StoreElement> answer(final InvocationOnMock invocationOnMock) throws Throwable {
				StoreElementFilter filter = (StoreElementFilter) invocationOnMock.getArguments()[0];
				Listable<StoreElement> storeElements = mock(Listable.class);

				if (filter.accept(templateFolderChild)) {
					when(storeElements.getFirst()).thenReturn(templateFolderChild);
				}

				return storeElements;
			}
		});

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("context", workflowScriptContext);
		new WfCheckPrerequisitesExecutable().execute(params);
	}

	@Test
	public void testDeleteSchemaWithReferences() throws IllegalAccessException {
		ReferenceEntry[] referenceEntries = new ReferenceEntry[] { getMockedReferenceEntry() };
		when(schema.getIncomingReferences()).thenReturn(referenceEntries);
		testDeleteSchema();
		verify(workflowScriptContext).doTransition("trigger_reference_conflict");
	}

	@Test
	public void testDeleteSchemaWithChildReferences() throws IllegalAccessException {
		final TableTemplate tableTemplate = mock(TableTemplate.class);
		ReferenceEntry[] referenceEntries = new ReferenceEntry[] { getMockedReferenceEntry() };
		when(tableTemplate.getIncomingReferences()).thenReturn(referenceEntries);

		when(schema.getIncomingReferences()).thenReturn(new ReferenceEntry[0]);
		when(schema.getChildren(any(TypedFilter.class), eq(true))).thenAnswer(new Answer<Listable<StoreElement>>() {
			public Listable<StoreElement> answer(final InvocationOnMock invocationOnMock) throws Throwable {
				TypedFilter<StoreElement> filter = (TypedFilter<StoreElement>) invocationOnMock.getArguments()[0];

				final List<StoreElement> children = new LinkedList<StoreElement>();
				Listable<StoreElement> childrenListable = new Listable<StoreElement>() {
					public StoreElement getFirst() {
						return children.size() > 0 ? children.get(0) : null;
					}

					public List<StoreElement> toList() {
						return new LinkedList<StoreElement>(children);
					}

					public Iterator<StoreElement> iterator() {
						return children.iterator();
					}
				};

				if (filter.accept(tableTemplate)) {
					children.add(tableTemplate);
				}

				return childrenListable;
			}
		});

		testDeleteSchema();
		verify(workflowScriptContext).doTransition("trigger_reference_conflict");
	}

	private ReferenceEntry getMockedReferenceEntry() {
		IDProvider referenceElement = mock(IDProvider.class);
		when(referenceElement.hasUid()).thenReturn(true);
		when(referenceElement.getDisplayName(language)).thenReturn("Mock ReferenceEntry");
		when(referenceElement.getUid()).thenReturn("Uid");
		when(referenceElement.getId()).thenReturn(0l);

		ReferenceEntry incomingReference = mock(ReferenceEntry.class);
		when(incomingReference.getReferencedElement()).thenReturn(referenceElement);

		return incomingReference;
	}

	private void testDeleteSchema() {
		setMockWorkflowable(schema);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("context", workflowScriptContext);
		new WfFindRelatedObjectsExecutable().execute(params);
	}

	private void setMockWorkflowable(Workflowable mockWorkflowable) {
		when(workflowScriptContext.getWorkflowable()).thenReturn(mockWorkflowable);
	}
}
