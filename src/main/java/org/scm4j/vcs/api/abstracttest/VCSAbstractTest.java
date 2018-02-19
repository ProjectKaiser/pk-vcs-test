package org.scm4j.vcs.api.abstracttest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.scm4j.vcs.api.*;
import org.scm4j.vcs.api.exceptions.EVCSBranchExists;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public abstract class VCSAbstractTest {
	protected static final String TEST_BASE_DIR = new File(System.getProperty("java.io.tmpdir"), "scm4j-vcs-test").getPath();
	protected static final String REPO_DIR = new File(TEST_BASE_DIR, "base-repo").getPath();
	protected static final String WORKSPACE_DIR = new File(TEST_BASE_DIR, "workspaces").getPath();
	protected static final String TEST_DATA_GEN_WORKSAPCE_DIR = new File(TEST_BASE_DIR, "test-data-gen").getPath();
	protected static final String NEW_BRANCH = "new-branch";
	protected static final String NEW_BRANCH_2 = "new-branch-2";
	protected static final String CREATED_DST_BRANCH_COMMIT_MESSAGE = "created dst branch";
	protected static final String DELETE_BRANCH_COMMIT_MESSAGE = "deleted";
	protected static final String FILE1_NAME = "file1.txt";
	protected static final String FILE2_NAME = "file2.txt";
	protected static final String FILE3_IN_FOLDER_NAME = "folder/file3.txt";
	protected static final String MOD_FILE_NAME = "mod file.txt";
	protected static final String FILE1_ADDED_COMMIT_MESSAGE = FILE1_NAME + " file added";
	protected static final String FILE2_ADDED_COMMIT_MESSAGE = FILE2_NAME + " file added";
	protected static final String FILE3_ADDED_COMMIT_MESSAGE = FILE3_IN_FOLDER_NAME + " file added";
	protected static final String MOD_FILE_ADDED_COMMIT_MESSAGE = MOD_FILE_NAME + " file added";
	protected static final String FILE1_CONTENT_CHANGED_COMMIT_MESSAGE = FILE1_NAME + " content changed";
	protected static final String MOD_FILE_CONTENT_CHANGED_COMMIT_MESSAGE = MOD_FILE_NAME + " content changed";
	protected static final String MERGE_COMMIT_MESSAGE = "merged.";
	protected static final String LINE_1 = "line 1";
	protected static final String LINE_2 = "line 2";
	protected static final String LINE_3 = "line 3";
	protected static final String MOD_LINE_1= "original line";
	protected static final String MOD_LINE_2= "modified line";
	protected static final String TAG_MESSAGE_1 = "tag 1 message";
	protected static final String TAG_NAME_1 = "tag1_name";
	protected static final String TAG_MESSAGE_2 = "tag 2 message";
	protected static final String TAG_NAME_2 = "tag2_name";
	protected static final String TAG_MESSAGE_3 = "tag 3 message";
	protected static final String TAG_NAME_3 = "tag3_name";

	protected static final String CONTENT_CHANGED_COMMIT_MESSAGE = "content changed";
	protected static final String FILE2_REMOVED_COMMIT_MESSAGE = FILE2_NAME + " removed";
	protected static final Integer DEFAULT_COMMITS_LIMIT = 100;

	protected String repoName;
	protected String repoUrl;
	protected IVCSWorkspace localVCSWorkspace;
	protected IVCSRepositoryWorkspace localVCSRepo;
	protected IVCSRepositoryWorkspace mockedVCSRepo;
	protected IVCSLockedWorkingCopy mockedLWC;
	protected IVCS vcs;
	protected IVCS vcsTestDataGen;

	// TODO: make test repositories be accessed through login\pwd

	public IVCS getVcs() {
		return vcs;
	}

	public void setVcs(IVCS vcs) {
		this.vcs = vcs;
	}

	@After
	public void setUpAndTearDown() throws Exception {
		mockedLWC.close();
		FileUtils.deleteDirectory(new File(TEST_BASE_DIR));
	}

	@Before
	public void setUp() throws Exception {
		File testBaseDir = new File(TEST_BASE_DIR);
		if (testBaseDir.exists()) {
			FileUtils.deleteDirectory(testBaseDir);
		}
		
		repoName = "scm4j-vcs-" + getVCSTypeString() + "-testrepo";

		localVCSWorkspace = new VCSWorkspace(WORKSPACE_DIR);
		IVCSWorkspace localVCSGenWorkspace = new VCSWorkspace(TEST_DATA_GEN_WORKSAPCE_DIR);

		repoUrl = new File(REPO_DIR, repoName).toURI().toString().replace("file:/", "file:///");

		localVCSRepo = localVCSWorkspace.getVCSRepositoryWorkspace(repoUrl);
		mockedVCSRepo = Mockito.spy(localVCSWorkspace.getVCSRepositoryWorkspace(repoUrl));

		IVCSRepositoryWorkspace localVCSGenRepo = localVCSGenWorkspace.getVCSRepositoryWorkspace(repoUrl);
		vcsTestDataGen = getVCS(localVCSGenRepo);

		vcs = getVCS(mockedVCSRepo);
		
		resetMocks();
		
		setMakeFailureOnVCSReset(false);
	}

	protected void resetMocks() throws Exception {
		if (mockedLWC != null) {
			mockedLWC.close();
		}
		Mockito.reset(mockedVCSRepo);
		mockedLWC = Mockito.spy(localVCSRepo.getVCSLockedWorkingCopy());
		Mockito.doReturn(mockedLWC).when(mockedVCSRepo).getVCSLockedWorkingCopy();
	}

	@Test
	public void testBranches() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getBranches("").contains(NEW_BRANCH));
		verifyMocks();
		assertTrue(vcs.getBranches(null).contains(NEW_BRANCH));
		verifyMocks();
		assertTrue(vcs.getFileContentFromBranch(NEW_BRANCH, FILE3_IN_FOLDER_NAME).equals(LINE_1));
		resetMocks();

		vcsTestDataGen.createBranch(NEW_BRANCH, NEW_BRANCH_2, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getBranches("").contains(NEW_BRANCH));
		verifyMocks();
		assertTrue(vcs.getBranches("").contains(NEW_BRANCH_2));
		verifyMocks();

		try {
			vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
			fail("\"Branch exists\" situation not detected");
		} catch (EVCSBranchExists e) {
		}

		// Check out the branch which will be deleted
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		resetMocks();
		vcsTestDataGen.deleteBranch(NEW_BRANCH, DELETE_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertFalse(vcs.getBranches("").contains(NEW_BRANCH));

		// check branch prefixing
		assertTrue(vcs.getBranches(NEW_BRANCH.substring(0, 5)).contains(NEW_BRANCH_2));
	}

	private void verifyMocks() throws Exception {
		try {
			Mockito.verify(mockedVCSRepo).getVCSLockedWorkingCopy();
		} catch (WantedButNotInvoked e) {
			resetMocks();
			return;
		}
		Mockito.verify(mockedLWC).close();
		resetMocks();
	}

	@Test
	public void testFileGetSetContent() throws Exception {
		VCSCommit commit = vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(logContainsMessage(null, FILE3_ADDED_COMMIT_MESSAGE));
		verifyMocks();
		assertEquals(vcs.getFileContentFromBranch(null, FILE3_IN_FOLDER_NAME), LINE_1);
		verifyMocks();

		vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_2, CONTENT_CHANGED_COMMIT_MESSAGE);
		assertEquals(vcs.getFileContentFromBranch(null, FILE3_IN_FOLDER_NAME), LINE_2);

		resetMocks();
		assertEquals(vcs.getFileContentFromRevision(commit.getRevision(), FILE3_IN_FOLDER_NAME), LINE_1);
		verifyMocks();

		try {
			vcs.getFileContentFromBranch(null, "sdfsdf1.txt");
			fail(EVCSFileNotFound.class.getSimpleName() + " is not thrown");
		} catch (EVCSFileNotFound e) {
		}
		
		try {
			vcs.getFileContentFromBranch("wrong-branch", FILE3_IN_FOLDER_NAME) ;
			fail(EVCSBranchNotFound.class.getSimpleName() + " is not thrown");
		} catch (EVCSBranchNotFound e) {
		}
	}
	
	@Test
	public void testFileSetContents() throws Exception {
		vcsTestDataGen.setFileContent(null, Arrays.asList(
				new VCSChangeListNode(FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE),
				new VCSChangeListNode(FILE3_IN_FOLDER_NAME, LINE_2, FILE3_ADDED_COMMIT_MESSAGE)));
		verifyMocks();
		List<VCSCommit> commits = vcs.getCommitsRange(null, null, WalkDirection.DESC, 1);
		assertTrue(commits.size() == 1);
		VCSCommit lastCommit = commits.get(0);
		assertTrue(lastCommit.getLogMessage().contains(FILE1_ADDED_COMMIT_MESSAGE));
		assertTrue(lastCommit.getLogMessage().contains(FILE3_ADDED_COMMIT_MESSAGE));
		assertEquals(LINE_1, vcs.getFileContentFromBranch(null, FILE1_NAME));
		assertEquals(LINE_2, vcs.getFileContentFromBranch(null, FILE3_IN_FOLDER_NAME));
		
		assertNull(vcsTestDataGen.setFileContent(null, new ArrayList<>()));
	}

	@Test
	public void testMerge() throws Exception {
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();

		VCSMergeResult res = vcs.merge(NEW_BRANCH, null, MERGE_COMMIT_MESSAGE);

		verifyMocks();
		assertFalse(mockedLWC.getCorrupted());
		assertTrue(res.getSuccess());
		assertTrue(res.getConflictingFiles().size() == 0);
		String content = vcs.getFileContentFromBranch(null, FILE1_NAME);

		assertEquals(content, LINE_1);
		content = vcs.getFileContentFromBranch(null, FILE2_NAME);
		assertEquals(content, LINE_2);
	}

	@Test
	public void testMergeConflict() {
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		VCSMergeResult res = vcs.merge(NEW_BRANCH, null, MERGE_COMMIT_MESSAGE);
		assertFalse(res.getSuccess());
		assertFalse(mockedLWC.getCorrupted());
		assertTrue(res.getConflictingFiles().size() == 1);
		assertTrue(res.getConflictingFiles().contains(FILE1_NAME));
	}

	@Test
	public void testMergeConflictWCCorruption() throws Exception {
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();
		setMakeFailureOnVCSReset(true);
		VCSMergeResult res = vcs.merge(NEW_BRANCH, null, MERGE_COMMIT_MESSAGE);
		assertFalse(res.getSuccess());
		assertTrue(mockedLWC.getCorrupted());
		assertTrue(res.getConflictingFiles().size() == 1);
		assertTrue(res.getConflictingFiles().contains(FILE1_NAME));
		assertFalse(mockedLWC.getFolder().exists());
		assertFalse(mockedLWC.getLockFile().exists());
	}
	
	@Test
	public void testBranchesDiff() throws Exception {
		/**
		 * Master Branch
		 *  f1-     
		 *   |     f2-
		 *   |     mfm
		 *   |     mf+ (merge)
		 *   |   /
		 *  mf+  
		 *   |     tf+ (merge) 
		 *   |   /  |
		 *  tf+    f1m
		 *   |     f3+
		 *  f2+  /     	 
		 *  f1+
		 *  
		 *  Result should be: f3+, f1+, f2-, mfm.
		 *  But: Result of merge operation for f1 is missing file even by TortouiseSVN 
		 */
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE2_NAME, LINE_1, FILE2_ADDED_COMMIT_MESSAGE);

		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE3_IN_FOLDER_NAME, LINE_2, FILE3_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_3, FILE1_CONTENT_CHANGED_COMMIT_MESSAGE);

		vcsTestDataGen.setFileContent(null, "trunk file.txt", "dfdfsdf", "trunk file added");

		vcsTestDataGen.setFileContent(null, MOD_FILE_NAME, MOD_LINE_1, MOD_FILE_ADDED_COMMIT_MESSAGE);

		vcsTestDataGen.merge(null, NEW_BRANCH, MERGE_COMMIT_MESSAGE);

		vcsTestDataGen.setFileContent(NEW_BRANCH, MOD_FILE_NAME, MOD_LINE_2, MOD_FILE_CONTENT_CHANGED_COMMIT_MESSAGE);

		vcsTestDataGen.merge(null, NEW_BRANCH, "merged from trunk");

		vcsTestDataGen.removeFile(NEW_BRANCH, FILE2_NAME, FILE2_REMOVED_COMMIT_MESSAGE);

		vcsTestDataGen.removeFile(null,  FILE1_NAME, "file1 removed");
		
		//vcs.setFileContent(null, "folder/file 2 in folder.txt", "file 2 in folder line", "conflicting folder added");
		vcsTestDataGen.setFileContent(null, "moved file trunk.txt", "file 2 in folder line", "moved file added");
		//vcs.merge(null, NEW_BRANCH, "merged moved file trunk.txt from trunk");
		
		
		resetMocks();
		List<VCSDiffEntry> diffs = vcs.getBranchesDiff(NEW_BRANCH, null);
		verifyMocks();
		assertNotNull(diffs);
		VCSDiffEntry diff;
		
		diff = getEntryDiffForFile(diffs, FILE3_IN_FOLDER_NAME);
		assertNotNull(diff);
		assertTrue(diff.getChangeType() == VCSChangeType.ADD);
		assertTrue(diff.getUnifiedDiff().contains("+" + LINE_2));
		
		diff = getEntryDiffForFile(diffs, MOD_FILE_NAME);
		assertNotNull(diff);
		assertTrue(diff.getChangeType() == VCSChangeType.MODIFY);
		assertTrue(diff.getUnifiedDiff().contains("-" + MOD_LINE_1));
		assertTrue(diff.getUnifiedDiff().contains("+" + MOD_LINE_2));
		
		diff = getEntryDiffForFile(diffs, FILE1_NAME);
		assertNotNull(diff);
		assertTrue(diff.getChangeType() == VCSChangeType.ADD);
		assertTrue(diff.getUnifiedDiff().contains("+" + LINE_3));
		
		diff = getEntryDiffForFile(diffs, FILE2_NAME);
		assertNotNull(diff);
		assertTrue(diff.getChangeType() == VCSChangeType.DELETE);
		assertTrue(diff.getUnifiedDiff().contains("-" + LINE_1));
	}
	
	private VCSDiffEntry getEntryDiffForFile(List<VCSDiffEntry> entries, String filePath) {
		for (VCSDiffEntry entry : entries) {
			if (entry.getFilePath().equals(filePath)) {
				return entry;
			}
		}
		return null;
	}

	@Test
	public void testFileRemove() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		vcs.removeFile(null, FILE3_IN_FOLDER_NAME, FILE2_REMOVED_COMMIT_MESSAGE);
		verifyMocks();
		try {
			vcs.getFileContentFromBranch(null, FILE3_IN_FOLDER_NAME);
			fail();
		} catch (EVCSFileNotFound e) {
		}
		
		assertTrue(logContainsMessage(null, FILE2_REMOVED_COMMIT_MESSAGE));
	}

	@Test
	public void testLog() throws Exception {
		VCSCommit c1 = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		VCSCommit c2 = vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_3, FILE3_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		VCSCommit c3 = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();
		List<VCSCommit> log = vcs.log(null, DEFAULT_COMMITS_LIMIT);
		verifyMocks();
		assertThat(new Object[] {log.get(0), log.get(1)}, is(new Object[] {c2, c1}));
		
		log = vcs.log(null, 1);
		assertTrue(log.size() == 1);
		assertThat(log.get(0), is(c2));
		
		log = vcs.log(NEW_BRANCH, 0);
		assertTrue(log.size() > 1);
		assertThat(log, hasItem(c3));
	}
	
	@Test 
	public void testCommitsGetRange() throws Exception {
		/**
		 * Master Branch
		 * 
		 *        f11+
		 *        f2+
		 *  f5+    |  
		 *  f4+    |
		 *   |   / 
		 *  f3+       	
		 *  f1+ 
		 */
		String c1 = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE).getRevision();
		String c3 = vcsTestDataGen.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_3, FILE3_ADDED_COMMIT_MESSAGE).getRevision();
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		String c4 = vcsTestDataGen.setFileContent(null, "file 4.txt", "dfdfsdf", "File 4 master added").getRevision();
		String c5 = vcsTestDataGen.setFileContent(null, "file 5.txt", "dfdfsdf", "File 5 master added").getRevision();
		String c2 = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE).getRevision();
		String c11 = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_2, "file 1 branch added").getRevision();

		resetMocks();
		List<VCSCommit> commits = vcs.getCommitsRange(null, c1, null);
		verifyMocks();
		assertTrue(commitsConsistsOfIds(commits, c3, c4, c5));
		
		commits = vcs.getCommitsRange(null, null, null);
		assertTrue(commitsConsistsOfIds(commits, c3, c4, c5));
		
		commits = vcs.getCommitsRange(NEW_BRANCH, c1, null);
		assertTrue(commitsContainsIds(commits, c2, c11));
		
		commits = vcs.getCommitsRange(null, c1, c4);
		assertTrue(commitsConsistsOfIds(commits, c3, c4));


		resetMocks();
		commits = vcs.getCommitsRange(null, c1, WalkDirection.ASC, 0);
		verifyMocks();
		assertTrue(commitsContainsSequenceOfIds(commits, c1, c3, c4, c5));

		commits = vcs.getCommitsRange(null, c1, WalkDirection.ASC, 2);
		assertTrue(commitsContainsSequenceOfIds(commits, c1, c3));
		assertTrue(commits.get(0).getRevision().equals(c1));

		commits = vcs.getCommitsRange(null, null, WalkDirection.ASC, 0);
		assertTrue(commitsContainsSequenceOfIds(commits, c1, c3, c4, c5));

		commits = vcs.getCommitsRange(NEW_BRANCH, c1, WalkDirection.ASC, 0);
		assertTrue(commitsContainsSequenceOfIds(commits, c2, c11));
		
		commits = vcs.getCommitsRange(null, c1, WalkDirection.ASC, Integer.MAX_VALUE);
		assertTrue(commitsContainsSequenceOfIds(commits, c1, c3, c4, c5));
		assertTrue(commits.get(0).getRevision().equals(c1));


		commits = vcs.getCommitsRange(null, c5, WalkDirection.DESC, 0);
		assertTrue(commitsContainsSequenceOfIds(commits, c5, c4, c3, c1));

		commits = vcs.getCommitsRange(null, c1, WalkDirection.DESC, 1);
		assertTrue(commits.get(0).getRevision().equals(c1));

		commits = vcs.getCommitsRange(null, null, WalkDirection.DESC, 0);
		assertTrue(commitsContainsSequenceOfIds(commits, c5, c4, c3, c1));
		assertTrue(commits.get(0).getRevision().equals(c5));

		commits = vcs.getCommitsRange(NEW_BRANCH, c11, WalkDirection.DESC, 0);
		assertTrue(commitsContainsSequenceOfIds(commits, c11, c2));
		assertTrue(commits.get(0).getRevision().equals(c11));
	}

	@Test
	public void testCommitGetHead() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		VCSCommit commit2 = vcsTestDataGen.setFileContent(null, FILE2_NAME, LINE_1, FILE2_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		VCSCommit commit3 = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE3_IN_FOLDER_NAME, LINE_2, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		assertTrue(vcs.getHeadCommit(null).equals(commit2));
		verifyMocks();
		assertTrue(vcs.getHeadCommit(NEW_BRANCH).equals(commit3));
		assertNull(vcs.getHeadCommit("wrong branch"));
	}
	
	@Test
	public void testFileExists() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE3_IN_FOLDER_NAME, LINE_2, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		assertTrue(vcs.fileExists(null, FILE1_NAME));
		verifyMocks();
		assertTrue(vcs.fileExists(NEW_BRANCH, FILE3_IN_FOLDER_NAME));
		assertFalse(vcs.fileExists(null, "no file"));
	}
	
	@Test
	public void testTagCreate() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		VCSCommit initialCommit = vcsTestDataGen.setFileContent(null, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();
		VCSTag ethalonTag = vcs.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
		verifyMocks();
		assertEquals(ethalonTag.getRelatedCommit(), initialCommit);
		assertEquals(ethalonTag.getTagMessage(), TAG_MESSAGE_1);
		assertEquals(ethalonTag.getTagName(), TAG_NAME_1);
		assertEquals(ethalonTag.getAuthor(), initialCommit.getAuthor());
		Thread.sleep(1000); // FIXME: testTagCreate() fails with no sleep(1000) on Git
		try {
			vcs.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
			fail();
		} catch (EVCSTagExists e) {
			
		}

		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		initialCommit = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_1, FILE2_ADDED_COMMIT_MESSAGE);
		ethalonTag = vcs.createTag(NEW_BRANCH, TAG_NAME_2, TAG_MESSAGE_2, null);
		assertEquals(ethalonTag.getRelatedCommit(), initialCommit);
		assertEquals(ethalonTag.getTagMessage(), TAG_MESSAGE_2);
		assertEquals(ethalonTag.getTagName(), TAG_NAME_2);
		assertEquals(ethalonTag.getAuthor(), initialCommit.getAuthor());
		Thread.sleep(1000);
		try {
			vcs.createTag(NEW_BRANCH, TAG_NAME_2, TAG_MESSAGE_2, null);
			fail();
		} catch (EVCSTagExists e) {
			
		}
	}
	
	@Test
	public void testTagListAfterDelete() throws Exception {
		vcsTestDataGen.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
		vcs.removeTag(TAG_NAME_1);
		assertTrue(vcs.getTags().isEmpty());
		assertTrue(vcsTestDataGen.getTags().isEmpty());
		VCSTag tag = vcsTestDataGen.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
		assertTrue(vcs.getTags().contains(tag));
	}
	
	@Test
	public void testTagsList() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		VCSTag ethalonTag1 = vcsTestDataGen.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_1, FILE2_ADDED_COMMIT_MESSAGE);
		VCSTag ethalonTag2 = vcsTestDataGen.createTag(NEW_BRANCH, TAG_NAME_2, TAG_MESSAGE_2, null);
		resetMocks();
		List<VCSTag> tags = vcs.getTags();
		verifyMocks();
		assertTrue(tags.size() == 2);
		tags.containsAll(Arrays.asList(ethalonTag1, ethalonTag2));
	}

	@Test
	public void testToString() {
		assertTrue(vcs.toString().contains(repoUrl));
	}

	@Test
	public void testRemoveTag() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, null);
		resetMocks();
		vcs.removeTag(TAG_NAME_1);
		verifyMocks();
		assertFalse(containsTagName(vcs.getTags(), TAG_NAME_1));
	}
	
	@Test
	public void testCheckoutHead() throws Exception {
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		IVCSRepositoryWorkspace rw = localVCSWorkspace.getVCSRepositoryWorkspace("test_checkout_place");
		try (IVCSLockedWorkingCopy lwc = rw.getVCSLockedWorkingCopy()) {
			lwc.setCorrupted(true);
			resetMocks();
			vcs.checkout(null, lwc.getFolder().getPath(), null);
			verifyMocks();
			File testFile = new File(lwc.getFolder(), FILE1_NAME);
			assertTrue(testFile.exists());
			assertEquals(FileUtils.readFileToString(testFile, StandardCharsets.UTF_8), LINE_1);
		}
	}
	
	@Test
	public void testCheckoutRevision() throws Exception {
		VCSCommit first = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_2, FILE1_CONTENT_CHANGED_COMMIT_MESSAGE);
		IVCSRepositoryWorkspace rw = localVCSWorkspace.getVCSRepositoryWorkspace("test_checkout_place");
		
		try (IVCSLockedWorkingCopy lwc = rw.getVCSLockedWorkingCopy()) {
			lwc.setCorrupted(true);
			vcs.checkout(null, lwc.getFolder().getPath(), first.getRevision());
			File testFile = new File(lwc.getFolder(), FILE1_NAME);
			assertTrue(testFile.exists());
			assertEquals(FileUtils.readFileToString(testFile, StandardCharsets.UTF_8), LINE_1);
		}
	}

	@Test
	public void testGetTagsOnRevision() throws Exception {
		VCSCommit c1 = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_2, FILE1_ADDED_COMMIT_MESSAGE);
		assertTrue(vcs.getTagsOnRevision(c1.getRevision()).isEmpty());
		c1 = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		VCSCommit c2 = vcsTestDataGen.setFileContent(null, FILE1_NAME, LINE_2, FILE1_CONTENT_CHANGED_COMMIT_MESSAGE + " " + LINE_2);
		vcsTestDataGen.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		VCSCommit c3 = vcsTestDataGen.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_3, FILE1_CONTENT_CHANGED_COMMIT_MESSAGE + " " + LINE_3);

		VCSTag tag1 = vcsTestDataGen.createTag(null, TAG_NAME_1, TAG_MESSAGE_1, c1.getRevision());
		VCSTag tag2 = vcsTestDataGen.createTag(null, TAG_NAME_2, TAG_MESSAGE_2, c1.getRevision());
		VCSTag tag3 = vcsTestDataGen.createTag(NEW_BRANCH, TAG_NAME_3, TAG_MESSAGE_3, c3.getRevision());

		resetMocks();
		assertTrue(vcs.getTagsOnRevision(c1.getRevision()).containsAll(Arrays.asList(
				tag1, tag2)));
		verifyMocks();
		assertTrue(vcs.getTagsOnRevision(c2.getRevision()).isEmpty());
		assertTrue(vcs.getTagsOnRevision(c3.getRevision()).containsAll(Arrays.asList(
				tag3)));
	}
	
	private boolean containsTagName(List<VCSTag> tags, String tagName) {
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(tagName)) {
				return true;
			}
		}
		return false;
	}

	private Boolean commitsContainsIds(List<VCSCommit> commits, String... ids) {
		if (commits.size() == 0 || ids.length == 0) {
			return false;
		}
		Integer count = 0;
		for (String id : ids) {
			for(VCSCommit commit : commits) {
				if (commit.getRevision().equals(id)) {
					count++;
					break;
				}
			}
		}
		return count == ids.length;
	}

	private Boolean commitsContainsSequenceOfIds(List<VCSCommit> commits,String... ids) {
		if (commits.size() == 0 || ids.length == 0) {
			return false;
		}
		Integer idIndex = 0;
		for (VCSCommit commit : commits) {
			if (commit.getRevision().equals(ids[idIndex])) {
				idIndex++;
			} else if (idIndex != 0) {
				return false;
			}
			if (idIndex >= ids.length) {
				return true;
			}
		}
		return false;
	}
	
	private Boolean commitsConsistsOfIds(List<VCSCommit> commits, String... ids) {
		if (commits.size() == 0 || ids.length == 0) {
			return false;
		}
		Integer count = 0;
		Boolean found;
		for (String id : ids) {
			found = false;
			for(VCSCommit commit : commits) {
				if (commit.getRevision().equals(id)) {
					count++;
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return count == ids.length;
	}
	
	private boolean logContainsMessage(String branchName, String commitMessage) {
		List<VCSCommit> log = vcs.log(branchName, DEFAULT_COMMITS_LIMIT);
		for (VCSCommit commit : log) {
			if (commit.getLogMessage().equals(commitMessage)) {
				return true;
			}
		}
		return false;
	}
	
	protected abstract IVCS getVCS(IVCSRepositoryWorkspace mockedVCSRepo);

	protected abstract void setMakeFailureOnVCSReset(Boolean doMakeFailure) throws Exception;
	
	protected abstract String getVCSTypeString();
}
