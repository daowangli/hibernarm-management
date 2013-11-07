package org.hibernarm.management.test;

import java.io.IOException;
import java.text.ParseException;

import org.hibernarm.management.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

public class TestARMBean extends ArchetypeTestBase{
	   @Test
       public void testARM() throws ParseException, IOException{
//   	    ARM bean=new ARM();
//   		bean.setContent("第一次");
//   		bean.setName("��һ��");
//   		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//   		Date date=format.parse("2013-12-23 12:20:20");
//   		bean.setModifyTime(date);
		initial();
   		Session session=HibernateUtil.currentSession();
   		Transaction tx=session.getTransaction();
   		tx.begin();
   		session.save(arms.get("openEHR-EHR-OBSERVATION.blood_pressure.v1"));
   		tx.commit();
   		HibernateUtil.closeSession();
       }
}