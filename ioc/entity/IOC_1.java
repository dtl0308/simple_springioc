package ioc.entity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IOC_1 {
	
	private static final Map<String,Object> singletonObjects = new ConcurrentHashMap<String, Object>();
	
	private static final List<String> beanNames = new ArrayList<>();
	
	private static final Map<String,BeanDefinition> beanClassMap = new HashMap<String, BeanDefinition>();
	
	private static final Map<String,Object> earlySingletonObjects = new HashMap<String, Object>();
	
	private static final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
	
	private static final Map<String,Object> singletonFactories = new HashMap<>();
	
	public IOC_1(String location) throws Exception {
        loadBeans(location);
    }
	

	public Object getBean(String name) {
        Object bean = singletonObjects.get(name);
        return bean;
    }
	
	/***
	 * 遍历XML文件，解析bean标签
	 * @param location
	 * @throws Exception
	 */
	private void loadBeans(String location) throws Exception {
		
		// 加载 xml 配置文件
        InputStream inputStream = new FileInputStream(location);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputStream);
        Element root = doc.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        
        for (int i = 0; i < nodes.getLength(); i++) {
        	
        	BeanDefinition beanDefinition = new BeanDefinition();
			
        	Node node = nodes.item(i);
        	if (node instanceof Element) {
				Element ele = (Element) node;
				String id = ele.getAttribute("id");
	            String beanName = ele.getAttribute("class");
	            
	            beanDefinition.setId(id);
	            beanDefinition.setBeanName(beanName);
	            
	            NodeList propertyNodes = ele.getElementsByTagName("property");
	            List<Map<String,String>> propertyList = new ArrayList<>();
	            Map<String,String> propertyMap;
	            for (int j = 0; j < propertyNodes.getLength(); j++) {
	            	
	            	propertyMap = new HashMap<String, String>();
	            	Node propertyNode = propertyNodes.item(j);
	            	if (propertyNode instanceof Element) {
	            		Element propertyElement = (Element) propertyNode;
	            		String name = propertyElement.getAttribute("name");
	                    String value = propertyElement.getAttribute("value");
	                    propertyMap.put("propertyName", name);
	                    if (value!=null && value.length()>0) {
	                    	propertyMap.put("propertyValue", value);
	                    	propertyMap.put("propertyType", "string");
	    				}else{
	    					String ref = propertyElement.getAttribute("ref");
	    					propertyMap.put("propertyValue", ref);
	                    	propertyMap.put("propertyType", "ref");
	    				}
	            	}
	            	propertyList.add(propertyMap);
	            }
	            beanDefinition.setPropertyList(propertyList);
	            beanDefinition.setBeanClass(getBeanClass(beanName));
	            beanNames.add(id);
	            beanClassMap.put(id, beanDefinition);
			}
		}
        doLoadBeanDefinitions();
	}
	
	/**
	 * 遍历XML中配置的bean，进行实例化和IOC
	 * @throws Exception
	 */
	private void doLoadBeanDefinitions() throws Exception{
		for(String beanName:beanNames){
			
			BeanDefinition beanDefinition = beanClassMap.get(beanName);
			
			doGetBean(beanDefinition);
		}
	}
	
	private Object doGetBean(BeanDefinition beanDefinition) throws Exception{
		
		Object bean = null;
		
		String beanName = beanDefinition.getId();
		
		Object sharedInstance = getSingleton(beanName,true);
		if (sharedInstance !=null) {
			bean = sharedInstance;
		}else{
			Object singletonObject = getSingleton(beanDefinition);
			
			bean = singletonObject;
		}
		return bean;
	}
	
	/**
	 * 先从缓存中获取，如果未命中并且没有在创建中，返回NULL
	 * 如果Bean正在创建中，从工厂中先拿到Bean返回(还未填充属性)
	 * @param beanName
	 * @param allowEarlyReference
	 * @return
	 */
	private Object getSingleton(String beanName,boolean allowEarlyReference){
		Object beanObject = singletonObjects.get(beanName);
		
		if (beanObject == null && singletonsCurrentlyInCreation.contains(beanName)) {
			
			beanObject = earlySingletonObjects.get(beanName);
			if (beanObject ==null && allowEarlyReference) {
				Object singletonFactory = singletonFactories.get(beanName);
				if (singletonFactory != null) {
					beanObject = singletonFactory;
					earlySingletonObjects.put(beanName, beanObject);
					singletonFactories.remove(beanName);
				}
			}
		}
		return beanObject;
	}
	
	/**
	 * 先从缓存获取Bean，如果未命中，直接创建，并把创建完且注入完成的Bean放入缓存
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	private Object getSingleton(BeanDefinition beanDefinition) throws Exception{
		String beanName = beanDefinition.getId();
		Object singletonObject = singletonObjects.get(beanName);
		if (singletonObject == null) {
			singletonObject = createBean(beanDefinition);
			singletonObjects.put(beanName,singletonObject);
			singletonFactories.remove(beanName);
			earlySingletonObjects.remove(beanName);
		}
		return singletonObject;
	}
	
	
	/**
	 * 实际创建Bean的过程
	 * 先把自己放入singletonsCurrentlyInCreation，说明正在创建中
	 * 把创建好的实例放入工厂。singletonFactories
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	private Object createBean(BeanDefinition beanDefinition) throws Exception{
		String beanName = beanDefinition.getId();
		singletonsCurrentlyInCreation.add(beanName);
		Object bean = beanDefinition.getBeanClass().newInstance();
		if (!singletonObjects.containsKey(beanName)) {
			singletonFactories.put(beanName, bean);
			earlySingletonObjects.remove(beanName);
		}
		populateBean(bean, beanDefinition.getPropertyList());
		return bean;
	}
	
	
	/**
	 * 填充属性，如果property的值是一个ref，循环第一步doGetBean。
	 * @param bean
	 * @param pvs
	 * @throws Exception
	 */
	public void populateBean(Object bean,List<Map<String,String>> pvs) throws Exception{
		
		for (int i = 0; i < pvs.size(); i++) {
			Map<String,String> property = pvs.get(i);
			
			String propName = property.get("propertyName");
			String propValue = property.get("propertyValue");
			String propType = property.get("propertyType");
			
			Field declaredField = bean.getClass().getDeclaredField(propName);
            declaredField.setAccessible(true);
            
            if ("string".equals(propType)) {
            	declaredField.set(bean, propValue);
			}else{
				String beanName = propValue;
				Object beanObject = singletonObjects.get(beanName);
				
				if (beanObject!=null) {
					declaredField.set(bean,beanObject);
				}else{
					Object refBean = doGetBean(beanClassMap.get(beanName));
					declaredField.set(bean, refBean);
				}
			}
		}
	}
	
	/**
	 * 反射拿到Class对象
	 * @param className
	 * @return
	 */
	private Class<?> getBeanClass(String className){
		Class<?> beanClass = null;
        try {
        	beanClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        return beanClass;
	}
	

	public static void main(String[] args) throws Exception {
		
		String path = "D:\\Workspaces\\Netty\\src\\ioc\\ioc_1.xml";
		
		new IOC_1(path);
		
		Iterator<Entry<String, Object>> item = singletonObjects.entrySet().iterator();
		
		while(item.hasNext()){
			Entry<String, Object> next = item.next();
			if (next.getValue() instanceof User) {
				User user = (User) next.getValue();
				System.out.println("userId:"+user.getId());
				System.out.println("userName:"+user.getName());
				System.out.println("userRoleName:"+user.getRole().getName());
			}else{
				Role role = (Role) next.getValue();
				System.out.println("roleId:"+role.getId());
				System.out.println("roleName:"+role.getName());
			}
			System.out.println("-----------------");
		}
	}
}
