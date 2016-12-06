package com.projectkaiser.scm.vcs.api.abstracttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.WantedButNotInvoked;

import com.projectkaiser.scm.vcs.api.IVCS;
import com.projectkaiser.scm.vcs.api.VCSChangeType;
import com.projectkaiser.scm.vcs.api.VCSDiffEntry;
import com.projectkaiser.scm.vcs.api.VCSMergeResult;
import com.projectkaiser.scm.vcs.api.exceptions.EVCSBranchExists;
import com.projectkaiser.scm.vcs.api.exceptions.EVCSFileNotFound;
import com.projectkaiser.scm.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import com.projectkaiser.scm.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import com.projectkaiser.scm.vcs.api.workingcopy.IVCSWorkspace;
import com.projectkaiser.scm.vcs.api.workingcopy.VCSWorkspace;

public abstract class VCSAbstractTest {
	private static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "pk-vcs-workspaces";
	private static final String NEW_BRANCH = "new-branch";
	private static final String NEW_BRANCH_2 = "new-branch-2";
	private static final String CREATED_DST_BRANCH_COMMIT_MESSAGE = "created dst branch";
	private static final String DELETE_BRANCH_COMMIT_MESSAGE = "deleted";
	private static final String FILE1_NAME = "file1.txt";
	private static final String FILE2_NAME = "file2.txt";
	private static final String FILE3_IN_FOLDER_NAME = "folder/file3.txt";
	private static final String MOD_FILE_NAME = "mod file.txt";
	private static final String FILE1_ADDED_COMMIT_MESSAGE = FILE1_NAME + " file added";
	private static final String FILE2_ADDED_COMMIT_MESSAGE = FILE2_NAME + " file added";
	private static final String FILE3_ADDED_COMMIT_MESSAGE = FILE3_IN_FOLDER_NAME + " file added";
	private static final String MOD_FILE_ADDED_COMMIT_MESSAGE = MOD_FILE_NAME + " file added";
	private static final String FILE1_CONTENT_CHANGED_COMMIT_MESSAGE = FILE1_NAME + " content changed";
	private static final String MOD_FILE_CONTENT_CHANGED_COMMIT_MESSAGE = MOD_FILE_NAME + " content changed";
	private static final String MERGE_COMMIT_MESSAGE = "merged.";
	private static final String LINE_1 = "line 1";
	private static final String LINE_2 = "line 2";
	private static final String LINE_3 = "line 3";
	private static final String MOD_LINE_1= "original line";
	private static final String MOD_LINE_2= "modified line";

	private static final String CONTENT_CHANGED_COMMIT_MESSAGE = "content changed";
	private static final String FILE2_REMOVED_COMMIT_MESSAGE = FILE2_NAME + " removed";
	private static final Integer DEFAULT_COMMITS_LIMIT = 100; 
	
	protected String repoName;
	protected String repoUrl;
	protected IVCSWorkspace localVCSWorkspace;
	protected IVCSRepositoryWorkspace localVCSRepo;
	protected IVCSRepositoryWorkspace mockedVCSRepo;
	protected IVCSLockedWorkingCopy mockedLWC;
	protected IVCS vcs;

	public IVCS getVcs() {
		return vcs;
	}

	public void setVcs(IVCS vcs) {
		this.vcs = vcs;
	}

	@After
	public void setUpAndTearDown() throws Exception {
		mockedLWC.close();
		FileUtils.deleteDirectory(new File(WORKSPACE_DIR));
	}

	@Before
	public void setUp() throws Exception {
		FileUtils.deleteDirectory(new File(WORKSPACE_DIR));
		
		repoName = "pk-vcs-" + getVCSTypeString() + "-testrepo";

		String uuid = UUID.randomUUID().toString();
		repoName = (repoName + "_" + uuid);
		
		localVCSWorkspace = new VCSWorkspace(WORKSPACE_DIR);

		repoUrl = getTestRepoUrl() + repoName;

		localVCSRepo = localVCSWorkspace.getVCSRepositoryWorkspace(repoUrl);
		mockedVCSRepo = Mockito.spy(localVCSWorkspace.getVCSRepositoryWorkspace(repoUrl));
		
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
	public void testCreateAndDeleteBranch() throws Exception {
		vcs.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getBranches().contains(NEW_BRANCH));
		verifyMocks();
		assertTrue(vcs.getBranches().size() == 2); // Master + NEW_BRANCH, no Folder 
		verifyMocks();
		assertTrue(vcs.getFileContent(NEW_BRANCH, FILE3_IN_FOLDER_NAME).equals(LINE_1));
		resetMocks();
		
		vcs.createBranch(NEW_BRANCH, NEW_BRANCH_2, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getBranches().contains(NEW_BRANCH));
		verifyMocks();
		assertTrue(vcs.getBranches().contains(NEW_BRANCH_2));
		verifyMocks();
		assertTrue(vcs.getBranches().size() == 3);
		verifyMocks();

		try {
			vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
			fail("\"Branch exists\" situation not detected");
		} catch (EVCSBranchExists e) {
		}

		resetMocks();
		vcs.deleteBranch(NEW_BRANCH, DELETE_BRANCH_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getBranches().size() == 2);
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
	public void testGetSetFileContent() throws Exception {
		vcs.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		verifyMocks();
		assertTrue(vcs.getCommitMessages(null, DEFAULT_COMMITS_LIMIT).contains(FILE3_ADDED_COMMIT_MESSAGE));
		verifyMocks();
		assertEquals(vcs.getFileContent(null, FILE3_IN_FOLDER_NAME), LINE_1);
		verifyMocks();
		assertEquals(vcs.getFileContent(null, FILE3_IN_FOLDER_NAME, "UTF-8"), LINE_1);
		verifyMocks();
		vcs.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_2, CONTENT_CHANGED_COMMIT_MESSAGE);
		assertEquals(vcs.getFileContent(null, FILE3_IN_FOLDER_NAME), LINE_2);
		assertEquals(vcs.getFileContent(null, FILE3_IN_FOLDER_NAME, "UTF-8"), LINE_2);
		
		try {
			vcs.getFileContent(null, "sdfsdf1.txt");
			fail("EVCSFileNotFound is not thrown");
		} catch (EVCSFileNotFound e) {
		}
	}

	@Test
	public void testMerge() throws Exception {
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcs.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();

		VCSMergeResult res = vcs.merge(NEW_BRANCH, null, MERGE_COMMIT_MESSAGE);

		verifyMocks();
		assertFalse(mockedLWC.getCorrupted());
		assertTrue(res.getSuccess());
		assertTrue(res.getConflictingFiles().size() == 0);
		String content = vcs.getFileContent(null, FILE1_NAME);

		assertEquals(content, LINE_1);
		content = vcs.getFileContent(null, FILE2_NAME);
		assertEquals(content, LINE_2);
	}

	@Test
	public void testMergeConflict() throws Exception {
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcs.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		VCSMergeResult res = vcs.merge(NEW_BRANCH, null, MERGE_COMMIT_MESSAGE);
		assertFalse(res.getSuccess());
		assertFalse(mockedLWC.getCorrupted());
		assertTrue(res.getConflictingFiles().size() == 1);
		assertTrue(res.getConflictingFiles().contains(FILE1_NAME));
	}

	@Test
	public void testMergeConflictWCCorruption() throws Exception {
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcs.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
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
		 *  Result should be: f3+, f1+, f2-.
		 */
		vcs.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(null, FILE2_NAME, LINE_1, FILE2_ADDED_COMMIT_MESSAGE);
		
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE3_IN_FOLDER_NAME, LINE_2, FILE3_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE1_NAME, LINE_3, FILE1_CONTENT_CHANGED_COMMIT_MESSAGE);
		
		vcs.setFileContent(null, "trunk file.txt", "dfdfsdf", "trunk file added");
		
		vcs.setFileContent(null, MOD_FILE_NAME, MOD_LINE_1, MOD_FILE_ADDED_COMMIT_MESSAGE);
		
		vcs.merge(null, NEW_BRANCH, MERGE_COMMIT_MESSAGE);
		
		vcs.setFileContent(NEW_BRANCH, MOD_FILE_NAME, MOD_LINE_2, MOD_FILE_CONTENT_CHANGED_COMMIT_MESSAGE);
		
		vcs.merge(null, NEW_BRANCH, "merged from trunk");
		
		vcs.removeFile(NEW_BRANCH, FILE2_NAME, FILE2_REMOVED_COMMIT_MESSAGE);
		
		vcs.removeFile(null,  FILE1_NAME, "file1 removed");
		
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
	public void testRemoveFile() throws Exception {
		vcs.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_1, FILE3_ADDED_COMMIT_MESSAGE);
		resetMocks();
		vcs.removeFile(null, FILE3_IN_FOLDER_NAME, FILE2_REMOVED_COMMIT_MESSAGE);
		verifyMocks();
		try {
			vcs.getFileContent(null, FILE3_IN_FOLDER_NAME);
			fail();
		} catch (EVCSFileNotFound e) {
		}
		
		List<String> commits = vcs.getCommitMessages(null, DEFAULT_COMMITS_LIMIT);
		assertTrue(commits.contains(FILE2_REMOVED_COMMIT_MESSAGE));
	}
	
	@Test
	public void testGetCommitMessages() throws Exception {
		vcs.setFileContent(null, FILE1_NAME, LINE_1, FILE1_ADDED_COMMIT_MESSAGE);
		vcs.setFileContent(null, FILE3_IN_FOLDER_NAME, LINE_3, FILE3_ADDED_COMMIT_MESSAGE);
		vcs.createBranch(null, NEW_BRANCH, CREATED_DST_BRANCH_COMMIT_MESSAGE);
		vcs.setFileContent(NEW_BRANCH, FILE2_NAME, LINE_2, FILE2_ADDED_COMMIT_MESSAGE);
		resetMocks();
		List<String> commits = vcs.getCommitMessages(null, DEFAULT_COMMITS_LIMIT);
		verifyMocks();
		assertTrue(commits.contains(FILE1_ADDED_COMMIT_MESSAGE));
		assertTrue(commits.contains(FILE3_ADDED_COMMIT_MESSAGE));
		commits = vcs.getCommitMessages(null, 1);
		assertFalse(commits.contains(FILE1_ADDED_COMMIT_MESSAGE));
		commits = vcs.getCommitMessages(NEW_BRANCH, DEFAULT_COMMITS_LIMIT);
		assertTrue(commits.contains(FILE2_ADDED_COMMIT_MESSAGE));
	}

	protected abstract String getTestRepoUrl();
	
	protected abstract IVCS getVCS(IVCSRepositoryWorkspace mockedVCSRepo);

	protected abstract void setMakeFailureOnVCSReset(Boolean doMakeFailure);
	
	protected abstract String getVCSTypeString();
}
