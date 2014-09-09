package demo;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * 
 * @作者： http://www.wenboxz.com
 * @功能：Java中进行Snmp编程Demo
 * @日期：2014-9-9
 */
public class SnmpDemo2 {
	/**
	 * Java中进行Snmp编程的一般步骤：
	 * 1）创建Snmp对象snmp；
	 * 2）创建CommunityTarget对象target，并指定community,version,address,timeout,retry等参数；
	 * 3）创建PDU对象pdu，并指定操纵类型（GET/GETNEXT/GETBULK/SET）,添加VariableBinding（也就是待操作的OID），
	 * 	    如果是GETBULK操作，还可以指定MaxRepetitions和NonRepeaters（注意：一定要指定MaxRepetitions，默认是0，那样
	 * 	    不会返回任何结果）；
	 * 4）调用snmp.send(pdu,target)方法，发送请求并返回结果
	 * 
	 */
	public static final int DEFAULT_VERSION = SnmpConstants.version2c;
	public static final String DEFAULT_PROTOCOL = "udp";
	public static final int DEFAULT_PORT = 161;
	public static final long DEFAULT_TIMEOUT = 3*1000L;
	public static final int DEFAULT_RETRY = 3;
	
	/**
	 * 1.创建CommunityTarget对象的方法；
	 * @param ip
	 * @param community
	 * @return
	 */
	public static CommunityTarget createDefault(String ip,String community){
		Address address = GenericAddress.parse(DEFAULT_PROTOCOL+":"+ip+"/"+DEFAULT_PORT);
		//创建target对象
		CommunityTarget target = new CommunityTarget();
		//指定团体名称community
		target.setCommunity(new OctetString(community));
		//指定地址address
		target.setAddress(address);
		//指定版本
		target.setVersion(DEFAULT_VERSION);
		//指定超时时间
		target.setTimeout(DEFAULT_TIMEOUT);
		//指定Retry
		target.setRetries(DEFAULT_RETRY);
		return target;
	}
	
	/**
	 * 2.判断刺探是否结束的方法
	 * @param targetOID
	 * @param pdu
	 * @param vb
	 * @return
	 */
	private static boolean checkWalkFinished(OID targetOID, PDU pdu,
			VariableBinding vb) {
		boolean finished = false;
		if (pdu.getErrorStatus() != 0) {
			System.out.println("[true] responsePDU.getErrorStatus() != 0 ");
			System.out.println(pdu.getErrorStatusText());
			finished = true;
		}else if(vb.getOid() == null){
			System.out.println("[true] vb.getOid() == null");
			finished = true;
		} else if (vb.getOid().size() < targetOID.size()) {
			System.out.println("[true] vb.getOid().size() < targetOID.size()");
			finished = true;
		} else if (targetOID.leftMostCompare(targetOID.size(), vb.getOid()) != 0) {
			System.out.println("[true] targetOID.leftMostCompare() != 0");
			finished = true;
		} else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
			System.out
					.println("[true] Null.isExceptionSyntax(vb.getVariable().getSyntax())");
			finished = true;
		} else if (vb.getOid().compareTo(targetOID) <= 0) {
			System.out.println("[true] Variable received is not "
					+ "lexicographic successor of requested " + "one:");
			System.out.println(vb.toString() + " <= " + targetOID);
			finished = true;
		}
		return finished;
	}
	
	/**
	 * 3.进行刺探的方法
	 * @param ip
	 * @param community
	 * @param targetOid
	 */
	public static void snmapwalk(String ip, String community, String targetOid){
		CommunityTarget target = createDefault(ip, community);
		TransportMapping transport = null;
		Snmp snmp =null;
		try{
			transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();
			PDU pdu = new PDU();
			OID targetOID = new OID(targetOid);
			pdu.add(new VariableBinding(targetOID));
			
			boolean finished = false;
			System.out.println("Demo Start .....");
			while(!finished){
				VariableBinding vb = null;
				ResponseEvent responseEvent = snmp.getNext(pdu, target);
				PDU response = responseEvent.getResponse();
				if(null == response){
					System.out.println("responsePDU == null ");
					finished = true;
					break;
				}else{
					vb = response.get(0);
				}
				//判断是否结束
				finished = checkWalkFinished(targetOID, pdu, vb);
				if (!finished) {
					System.out.println("==== walk each vlaue :");
					System.out.println(vb.getOid() + " = " + vb.getVariable());
					// Set up the variable binding for the next entry.
					pdu.setRequestID(new Integer32(0));
					pdu.set(0, vb);
				} else {
					System.out.println("SNMP walk OID has finished.");
					snmp.close();
				}
			}
			System.out.println("Demo end ....");
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("SNMP walk  Exception :" +e);
		}finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException ex1) {
					snmp = null;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String ip = "127.0.0.1";
		String community = "public";
		String targetOid = ".1.3.6.1.2.1.1";
		SnmpDemo2.snmapwalk(ip, community, targetOid);
	}
}
