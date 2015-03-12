package com.espirit.moddev.basicworkflows.util;

import de.espirit.common.util.ElementProvider;
import de.espirit.common.util.Filter;
import de.espirit.common.util.Listable;
import de.espirit.firstspirit.access.GenerationContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.Principal;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.UrlCreatorProvider;
import de.espirit.firstspirit.access.User;
import de.espirit.firstspirit.access.packagepool.Package;
import de.espirit.firstspirit.access.project.Group;
import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.access.project.TemplateSet;
import de.espirit.firstspirit.access.store.Data;
import de.espirit.firstspirit.access.store.ElementDeletedException;
import de.espirit.firstspirit.access.store.ElementMovedException;
import de.espirit.firstspirit.access.store.ExportHandler;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.ImportHandler;
import de.espirit.firstspirit.access.store.LanguageInfo;
import de.espirit.firstspirit.access.store.LockException;
import de.espirit.firstspirit.access.store.MultiPageParams;
import de.espirit.firstspirit.access.store.PageParams;
import de.espirit.firstspirit.access.store.Permission;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.Content2Params;
import de.espirit.firstspirit.access.store.sitestore.PageGroup;
import de.espirit.firstspirit.access.store.sitestore.PageLangSpec;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.SiteStoreFolder;
import de.espirit.firstspirit.access.store.templatestore.Workflow;
import de.espirit.firstspirit.access.store.templatestore.WorkflowLockException;
import de.espirit.firstspirit.access.store.templatestore.WorkflowPermission;
import de.espirit.firstspirit.base.store.PreviewUrl;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.storage.Contrast;
import de.espirit.firstspirit.storage.Revision;
import de.espirit.firstspirit.store.access.PermissionMap;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Created by Zaplatynski on 14.10.2014.
 */
public class FakePageRef implements PageRef {


    @Override
    public String getName() {
        return null;
    }

    @Override
    public Listable<StoreElement> getChildren() {
        return null;
    }

    @Override
    public <T extends StoreElement> Listable<T> getChildren(final Class<T> tClass) {
        return null;
    }

    @Override
    public <T extends StoreElement> Listable<T> getChildren(final Class<T> tClass, final boolean b) {
        return null;
    }

    @Override
    public <T extends StoreElement> Listable<T> getChildren(final Filter.TypedFilter<T> tTypedFilter, final boolean b) {
        return null;
    }

    @Override
    public void appendChild(final StoreElement storeElement) {

    }

    @Override
    public void appendChildBefore(final StoreElement storeElement, final StoreElement storeElement2) {

    }

    @Override
    public void removeChild(final StoreElement storeElement) {

    }

    @Override
    public void replaceChild(final StoreElement storeElement, final StoreElement storeElement2) {

    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int getChildIndex(final StoreElement storeElement) {
        return 0;
    }

    @Override
    public long getPageId() {
        return 0;
    }

    @Override
    public Page getPage() {
        return null;
    }

    @Override
    public void setPage(final Page page) {

    }

    @Override
    public int getPageGroupPosition() {
        return 0;
    }

    @Override
    public PageGroup getPageGroup() {
        return null;
    }

    @Override
    public void setPageGroup(final PageGroup pageGroup) {

    }

    @Override
    public PageLangSpec getPageLangSpec(final Language language) {
        return null;
    }

    @Override
    public Content2Params getContent2Params() {
        return null;
    }

    @Override
    public UidType getUidType() {
        return null;
    }

    @Override
    public boolean hasUid() {
        return false;
    }

    @Override
    public LanguageInfo getLanguageInfo(final Language language) {
        return null;
    }

    @Override
    public void moveChild(final IDProvider idProvider) throws LockException, ElementMovedException {

    }

    @Override
    public void moveChild(final IDProvider idProvider, final int i) throws LockException, ElementMovedException {

    }

    @Override
    public String getDisplayName(final Language language) {
        return null;
    }

    @Override
    public void setDisplayName(Language language, String s) {
    }

    @Override
    public boolean isReleaseSupported() {
        return false;
    }

    @Override
    public int getReleaseStatus() {
        return 0;
    }

    @Override
    public boolean isReleased() {
        return false;
    }

    @Override
    public User getReleasedBy() {
        return null;
    }

    @Override
    public boolean isInReleaseStore() {
        return false;
    }

    @Override
    public void release() {

    }

    @Override
    public void release(final boolean b) {

    }

    @Override
    public IDProvider getParent() {
        return null;
    }

    @Override
    public StoreElement getNextSibling() {
        return null;
    }

    @Override
    public StoreElement getFirstChild() {
        return null;
    }

    @Override
    public Store getStore() {
        return null;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public boolean isPermissionSupported() {
        return false;
    }

    @Override
    public boolean hasPermissions() {
        return false;
    }

    @Override
    public Permission getPermission() {
        return null;
    }

    @Override
    public Permission getPermission(final User user) {
        return null;
    }

    @Override
    public Permission getPermission(final Group group) {
        return null;
    }

    @Override
    public void setPermission(final User user, final Permission permission) {

    }

    @Override
    public void setPermission(final User[] users, final Permission permission) {

    }

    @Override
    public void setPermission(final Group group, final Permission permission) {

    }

    @Override
    public void removePermission(final User user) {

    }

    @Override
    public void removePermission(final User[] users) {

    }

    @Override
    public void removePermission(final Group group) {

    }

    @Override
    public PermissionMap getTreePermission() {
        return null;
    }

    @Override
    public List<Principal> getDefinedPrincipalPermissions() {
        return null;
    }

    @Override
    public List<Principal> getInheritedPrincipalPermissions() {
        return null;
    }

    @Override
    public long getLastChanged() {
        return 0;
    }

    @Override
    public User getEditor() {
        return null;
    }

    @Override
    public boolean isWorkflowSupported() {
        return false;
    }

    @Override
    public WorkflowPermission[] getWorkflowPermissions() {
        return new WorkflowPermission[0];
    }

    @Override
    public WorkflowPermission getWorkflowPermission(final Workflow workflow) {
        return null;
    }

    @Override
    public WorkflowPermission getCreateWorkflowPermission(final Workflow workflow) {
        return null;
    }

    @Override
    public void setWorkflowPermission(final WorkflowPermission workflowPermission) {

    }

    @Override
    public void setWorkflowPermissions(final WorkflowPermission[] workflowPermissions) {

    }

    @Override
    public void removeWorkflowPermission(final Workflow workflow) {

    }

    @Override
    public void removeAllWorkflowPermissions() {

    }

    @Override
    public boolean isWorkflowAllowed(final Workflow workflow, final User user) {
        return false;
    }

    @Override
    public boolean inheritWorkflowPermission() {
        return false;
    }

    @Override
    public void setInheritWorkflowPermission(final boolean b) {

    }

    @Override
    public void setWriteLock(final boolean b) {

    }

    @Override
    public boolean getWriteLock() {
        return false;
    }

    @Override
    public boolean isLockSupported() {
        return false;
    }

    @Override
    public void setLock(final boolean b) throws LockException, ElementDeletedException {

    }

    @Override
    public void setLock(final boolean b, final boolean b2) throws LockException, ElementDeletedException {

    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isLockedOnServer(final boolean b) {
        return false;
    }

    @Override
    public Set<Contrast> contrastWith(final IDProvider idProvider) {
        return null;
    }

    @Override
    public void revert(final Revision revision, final boolean b, final EnumSet<RevertType> revertTypes) throws LockException {

    }

    @Override
    public Data getMeta() {
        return null;
    }

    @Override
    public void setMeta(final Data dataValues) {

    }

    @Override
    public boolean hasMeta() {
        return false;
    }

    @Override
    public FormData getMetaFormData() {
        return null;
    }

    @Override
    public void setMetaFormData(final FormData formData) {

    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Long getLongID() {
        return null;
    }

    @Override
    public Revision getRevision() {
        return null;
    }

    @Override
    public Revision getReleaseRevision() {
        return null;
    }

    @Override
    public IDProvider getInRevision(final Revision revision) {
        return null;
    }

    @Override
    public String getUid() {
        return null;
    }

    @Override
    public void setUid(final String s) {

    }

    @Override
    public void createContent(final GenerationContext generationContext, final PageParams pageParams) throws IOException {

    }

    @Override
    public String getFilename() {
        return null;
    }

    @Override
    public String getExtension(final TemplateSet templateSet) {
        return null;
    }

    @Override
    public MultiPageParams getMultiPageParams(final Language language, final TemplateSet templateSet) {
        return null;
    }

    @Override
    public String getUrl(final UrlCreatorProvider urlCreatorProvider, final Language language, final TemplateSet templateSet,
                         final PageParams pageParams, final int i) {
        return null;
    }

    @Override
    public String getStoredUrl(final Language language, final TemplateSet templateSet, final Object o) {
        return null;
    }

    @Override
    public void setFilename(final String s) {

    }

    @Override
    public SiteStoreFolder getParentFolder() {
        return null;
    }

    @Override
    public void save() {

    }

    @Override
    public void save(final String s) {

    }

    @Override
    public void save(final String s, final boolean b) {

    }

    @Override
    public boolean hasTask() {
        return false;
    }

    @Override
    public Task getTask() {
        return null;
    }

    @Override
    public void setTask(final Task task) {

    }

    @Override
    public void removeTask() {

    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public void setColor(final Color color) {

    }

    @Override
    public void delete() throws LockException {

    }

    @Override
    public void refresh() {

    }

    @Override
    public String toXml() {
        return null;
    }

    @Override
    public String toXml(final boolean b) {
        return null;
    }

    @Override
    public String toXml(final boolean b, final boolean b2) {
        return null;
    }

    @Override
    public boolean isImportSupported() {
        return false;
    }

    @Override
    public boolean isExportSupported() {
        return false;
    }

    @Override
    public void exportStoreElement(final OutputStream outputStream, final ExportHandler exportHandler) throws IOException {

    }

    @Override
    public StoreElement importStoreElement(final ZipFile zipFile, final ImportHandler importHandler)
        throws IOException, ElementDeletedException, WorkflowLockException {
        return null;
    }

    @Override
    public Listable<StoreElement> importStoreElements(final ZipFile zipFile, final ImportHandler importHandler)
        throws IOException, ElementDeletedException, WorkflowLockException {
        return null;
    }

    @Override
    public void update(final ZipFile zipFile, final ImportHandler importHandler) throws IOException {

    }

    @Override
    public String getElementType() {
        return null;
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public ReferenceEntry[] getIncomingReferences() {
        return new ReferenceEntry[0];
    }

    @Override
    public boolean hasIncomingReferences() {
        return false;
    }

    @Override
    public ReferenceEntry[] getOutgoingReferences() {
        return new ReferenceEntry[0];
    }

    @Override
    public String getReferenceName() {
        return null;
    }

    @Override
    public Set<ReferenceEntry> getReferences() {
        return null;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isPackageItem() {
        return false;
    }

    @Override
    public boolean isSubscribedItem() {
        return false;
    }

    @Override
    public void addToPackage(final Package aPackage) throws LockException, ElementDeletedException {

    }

    @Override
    public void removeFromPackage(final Package aPackage) throws LockException, ElementDeletedException {

    }

    @Override
    public boolean isAddable(final Package aPackage) {
        return false;
    }

    @Override
    public boolean isChangeable() {
        return false;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public Package getPackage() {
        return null;
    }

    @Override
    public int getChangeState() {
        return 0;
    }

    @Override
    public void setChangeState(final int i) throws IllegalAccessException {

    }

    @Override
    public String getPreviewUrl(final Language language, final TemplateSet templateSet, final boolean b, final int i, final Map<?, ?> map) {
        return null;
    }

    @Override
    public PreviewUrl getPreviewUrlObject(final Language language, final TemplateSet templateSet, final boolean b, final int i, final Map<?, ?> map) {
        return null;
    }

    @Override
    public boolean isStartNode() {
        return false;
    }

    @Override
    public List<Revision> getHistory() {
        return null;
    }

    @Override
    public List<Revision> getHistory(final Date date, final Date date2, final int i, final Filter<Revision> revisionFilter) {
        return null;
    }

    @Override
    public ElementProvider<Revision> asRevisionProvider() {
        return null;
    }

    @Override
    public int compareTo(final StoreElement o) {
        return 0;
    }
}
