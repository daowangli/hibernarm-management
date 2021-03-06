package edu.zju.bme.hibernarm.management.archetype.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.zju.bme.hibernarm.management.control.action.HibernarmControl;
import edu.zju.bme.hibernarm.management.control.action.HibernarmControlAction;
import edu.zju.bme.hibernarm.management.dao.ARMBeanDao;
import edu.zju.bme.hibernarm.management.dao.ARMBeanDaoImpl;
import edu.zju.bme.hibernarm.management.dao.ArchetypeBeanDao;
import edu.zju.bme.hibernarm.management.dao.ArchetypeBeanDaoImpl;
import edu.zju.bme.hibernarm.management.dao.CommitSequenceDao;
import edu.zju.bme.hibernarm.management.dao.CommitSequenceDaoImpl;
import edu.zju.bme.hibernarm.management.exception.HibernarmValidationException;
import edu.zju.bme.hibernarm.management.exception.RestoreFileException;
import edu.zju.bme.hibernarm.management.exception.SaveFileException;
import edu.zju.bme.hibernarm.management.model.ARMBean;
import edu.zju.bme.hibernarm.management.model.ArchetypeBean;
import edu.zju.bme.hibernarm.management.model.CommitSequence;
import edu.zju.bme.hibernarm.management.util.ARMUtil;
import edu.zju.bme.hibernarm.management.util.ArchetypeUtil;
import edu.zju.bme.hibernarm.management.util.CommitSequenceConstant;
import edu.zju.bme.hibernarm.management.util.FileUtil;
import edu.zju.bme.hibernarm.management.util.HibernateUtil;
import edu.zju.bme.hibernarm.service.AQLExecute;

public class FileUploadAction {
	private static Logger logger = Logger.getLogger(FileUploadAction.class
			.getName());

	private File[] upload;
	private String[] uploadFileName;
	private String[] uploadContentType;
	private String uploadResult;
	private String uploadResultDescription;
	private static ArchetypeBeanDao archetypeBeanDao = new ArchetypeBeanDaoImpl();
	private static ARMBeanDao armBeanDao = new ARMBeanDaoImpl();
	private static CommitSequenceDao commitSequenceDao = new CommitSequenceDaoImpl();

	public File[] getUpload() {
		return upload;
	}

	public void setUpload(File[] upload) {
		this.upload = upload;
	}

	public String[] getUploadFileName() {
		return uploadFileName;
	}

	public void setUploadFileName(String[] uploadFileName) {
		this.uploadFileName = uploadFileName;
	}

	public String[] getUploadContentType() {
		return uploadContentType;
	}

	public void setUploadContentType(String[] uploadContentType) {
		this.uploadContentType = uploadContentType;
	}

	public String getUploadResult() {
		return uploadResult;
	}

	public void setUploadResult(String uploadResult) {
		this.uploadResult = uploadResult;
	}

	public String getUploadResultDescription() {
		return uploadResultDescription;
	}

	public void setUploadResultDescription(String uploadResultDescription) {
		this.uploadResultDescription = uploadResultDescription;
	}

	private static List<ArchetypeBean> constructArchetypeBeans(File[] upload,
			String[] uploadFileName, String[] uploadContentType,
			Date modifyTime, CommitSequence version) {
		List<ArchetypeBean> list = new ArrayList<ArchetypeBean>();
		for (int i = 0; i < upload.length; i++) {
			if (FileUtil.getFileType(upload[i]).compareToIgnoreCase("adl") == 0) {
				ArchetypeUtil archetypeUtil = new ArchetypeUtil(upload[i]);
				String archetypeId = archetypeUtil.getArchetypeId();
				String archetypeDescription = archetypeUtil.getArchetypeDescription();
				String archetypeContent = archetypeUtil.getArchetypeContent();
				if (!archetypeId.isEmpty() && !archetypeContent.isEmpty()) {
					ArchetypeBean archetypeBean = new ArchetypeBean();
					archetypeBean.setModifyTime(modifyTime);
					archetypeBean.setContent(archetypeContent);
					archetypeBean.setName(archetypeId);
					archetypeBean.setDescription(archetypeDescription);
					archetypeBean.setCommitSequence(version);
					list.add(archetypeBean);
				}
			}
		}
		return list;
	}

	private static List<ARMBean> constructArmBeans(File[] upload,
			String[] uploadFileName, String[] uploadContentType,
			Date modifyTime, CommitSequence version) throws Exception {
		List<ARMBean> list = new ArrayList<ARMBean>();
		for (int i = 0; i < uploadFileName.length; i++) {
			if (FileUtil.getFileType(upload[i]).compareToIgnoreCase("xml") == 0) {
				ARMUtil armUtil = new ARMUtil(upload[i]);
				String archetypeId = armUtil.getArchetypeId();
				String armContent = armUtil.getARMContent();
				if (!archetypeId.isEmpty() && !armContent.isEmpty()) {
					ARMBean armBean = new ARMBean();
					armBean.setContent(armContent);
					armBean.setModifyTime(modifyTime);
					armBean.setName(archetypeId);
					armBean.setCommitSequence(version);
					list.add(armBean);
				}
			}
		}
		return list;
	}

	public String execute() {
		uploadResult = "success";
		List<ArchetypeBean> listArchetypeBeans = null;
		List<ARMBean> listArmBeans = null;
		CommitSequence commitSequence = null;
		try {
			Date modifyTime = new Date(System.currentTimeMillis());
			commitSequence = new CommitSequence();
			commitSequence
					.setCommitValidation(CommitSequenceConstant.VALID);
			commitSequence.setCommitTime(modifyTime);
			commitSequenceDao.saveCommitSequence(commitSequence);
			listArchetypeBeans = constructArchetypeBeans(upload,
					uploadFileName, uploadContentType, modifyTime,
					commitSequence);
			listArmBeans = constructArmBeans(upload, uploadFileName,
					uploadContentType, modifyTime, commitSequence);
			saveAction(listArmBeans, listArchetypeBeans);
			validateHibernarm();
		} catch (SaveFileException e) {
			uploadResult = "fail";
			uploadResultDescription = "Error occured during save file to database";
			logger.error("Error occured during save file to database", e);
		} catch (HibernarmValidationException e) {
			uploadResult = "fail";
			uploadResultDescription = "Error occured during validate hibernarm";
			logger.error("Error occured during validate hibernarm", e);
			commitSequence
					.setCommitValidation(CommitSequenceConstant.INVALID);
			commitSequenceDao.saveCommitSequence(commitSequence);
			cancelAction(listArmBeans, listArchetypeBeans);
		} catch (Throwable e) {
			uploadResult = "fail";
			uploadResultDescription = "Error occured during upload file";
			logger.error("Error occured during upload file", e);
			commitSequence
					.setCommitValidation(CommitSequenceConstant.INVALID);
			commitSequenceDao.saveCommitSequence(commitSequence);
			cancelAction(listArmBeans, listArchetypeBeans);
		} finally {
			HibernateUtil.closeSession();
		}
		return "success";
	}

	protected void validateHibernarm() {
		try {
			@SuppressWarnings("resource")
			ApplicationContext context = new ClassPathXmlApplicationContext(
					"/beans.xml", HibernarmControlAction.class);
			AQLExecute client = (AQLExecute) context
					.getBean("wsclientvalidation");

			HibernarmControl control = new HibernarmControl();
			control.execute(client);
		} catch (Exception e) {
			throw new HibernarmValidationException(e);
		}
	}

	private void saveAction(List<ARMBean> listArmBeans,
			List<ArchetypeBean> listArchetypeBeans) throws Throwable {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			for (ARMBean armBean : listArmBeans) {
				armBeanDao.saveOrUpdate(armBean, session);
			}
			for (ArchetypeBean archetypeBean : listArchetypeBeans) {
				archetypeBeanDao.saveOrUpdate(archetypeBean, session);
			}
			tx.commit();
		} catch (Throwable e) {
			tx.rollback();
			throw new SaveFileException(e);
		}
	}

	private void cancelAction(List<ARMBean> listArmBeans,
			List<ArchetypeBean> listArchetypeBeans) {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			for (ArchetypeBean archetypeBean : listArchetypeBeans) {
				archetypeBeanDao.deleteAndRestore(archetypeBean, session);
			}
			for (ARMBean armBean : listArmBeans) {
				armBeanDao.deleteAndRestore(armBean, session);
			}
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			throw new RestoreFileException(e);
		}
	}
}
