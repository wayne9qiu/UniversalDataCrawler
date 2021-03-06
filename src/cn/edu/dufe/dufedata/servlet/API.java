package cn.edu.dufe.dufedata.servlet;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.bcel.generic.NEW;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.omg.CORBA.PRIVATE_MEMBER;

import cn.edu.dufe.dufedata.bean.LogBean;
import cn.edu.dufe.dufedata.controller.MainController;
import cn.edu.dufe.dufedata.controller.NodeController;
import cn.edu.dufe.dufedata.node.Node;
import cn.edu.dufe.dufedata.plugin.Plugin;
import cn.edu.dufe.dufedata.util.Json2CSV;
import cn.edu.dufe.dufedata.util.LogQueueUtil;

public class API extends HttpServlet {
	MainController controller=MainController.getInstance();
	NodeController nodeController=NodeController.getInstance();
	/**
	 * Constructor of the object.
	 */
	public API() {
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
		// Put your code here
	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("text/json");
		PrintWriter out = response.getWriter();
//		System.out.println(request.getParameter("cmd"));
		
		
//		out.print("{ \""+(Integer.valueOf(request.getParameter("last_no")))+"\" :"+"\"Nov 19 11:25:40 - [DEBUG] previous\\n\", \""+(Integer.valueOf(request.getParameter("last_no"))+1)+"\" :\"Nov 19 11:25:40 - [DEBUG] new\\n\"}");
//		System.out.println(request.getParameter("last_no"));
		out.flush();
		out.close();
		
		
		
	}
	
	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		request.setCharacterEncoding("utf-8");
		//接收不同的action，执行不同的动作
		if (request.getParameter("action").equals("start")) {
			out.println(startOnePlugin(request.getParameter("location"),request.getParameter("pluginID"), request.getParameter("param").split(" ")));
		}else if (request.getParameter("action").equals("stop")) {
			out.println(stopOnePlugin(request.getParameter("pluginID")));
		}else if (request.getParameter("action").equals("status")) {
			out.println(getStatus());
		}else if (request.getParameter("action").equals("delete")) {
			out.println(deleteFile(request.getParameter("fileName")));
		}else if (request.getParameter("action").equals("toCSV")) {
			File file=new File("WebRoot/data/"+request.getParameter("values"));
			String[] tmpStrings=request.getParameter("values").split("/");
			String pluginID="";
			System.out.println(tmpStrings.length);
			System.out.println(request.getParameter("values"));
			if (tmpStrings.length>=3) {
				pluginID=request.getParameter("values").split("/")[1];
			}
			out.println(toCSV(pluginID,file));
		}else if (request.getParameter("action").equals("log")) {
			int currentCursor=Integer.valueOf(request.getParameter("cursor"));
			out.print(getLog(currentCursor));
		}
		out.flush();
		out.close();
	}
	
	
	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {
		// Put your code here
	}
	
	//启动插件
	public String startOnePlugin(String loc,String pluginID,String[] args){
		JSONObject object=new JSONObject();
		if (loc.equals("local")) {
			Plugin plugin=controller.getOnePlugin(pluginID);
			if (plugin!=null) {
				try {
					controller.crawl(plugin, args);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				object.put("pluginID", plugin.getPluginID());
				object.put("status", "1");
				object.put("loc", "local");
				object.put("error", "");
			}else {
				object.put("pluginID", "");
				object.put("status", "-1");
				object.put("loc", "local");
				object.put("error", "invalid plugin");
			}
		}else {
			Node slave=nodeController.getOneSlave(loc);
			nodeController.startRemotePlugin(slave, pluginID, args);
			object.put("pluginID", pluginID);
			object.put("status", "1");
			object.put("error", "");
			object.put("loc", slave.getName());
		}
		
		return object.toString();
	}
	//停止插件
	public String stopOnePlugin(String pluginID){
		Plugin plugin=controller.getOnePlugin(pluginID);
		JSONObject object=new JSONObject(); 
		if (plugin!=null) {
			controller.stop(plugin);
			object.put("pluginID", plugin.getPluginID());
			object.put("status", plugin.getPluginStatus());
			object.put("error", "");
		}else {
			object.put("pluginID", "");
			object.put("status", "0");
			object.put("error", "invalid plugin");
		}
		return object.toString();
	}
	
	//获取状态
	public String getStatus(){
		
		ArrayList<Plugin> plugins=controller.getPlugins();
		JSONObject object=new JSONObject();
		JSONArray array=new JSONArray();
		for (Plugin plugin : plugins) {
			JSONObject object2=new JSONObject();
			object2.put("pluginID", plugin.getPluginID());
			object2.put("status", plugin.getPluginStatus());
			object2.put("loc", "local");
			array.put(object2);
		}
		//如果是分布式的话，会获得远程节点状态
		ArrayList<Node> slave=nodeController.getSlaves();
		for (Node node : slave) {
			Iterator<String> iterator=node.getPlugins().keySet().iterator();
			while (iterator.hasNext()) {
				String pluginID=iterator.next();
				JSONObject pluginObject=new JSONObject();
				pluginObject.put("pluginID", node.getPlugins().get(pluginID).getPluginID());
				pluginObject.put("status", node.getPlugins().get(pluginID).getPluginStatus());
				pluginObject.put("loc", node.getName());
				array.put(pluginObject);
			}
		}
		object.put("result", array);
		return object.toString();
	}
	
	//删除文件
	public String deleteFile(String fileName){
		JSONObject object;
		if (fileName.startsWith("log")) {
			File file=FileUtils.getFile(new File("WebRoot/log/"+fileName));
			object=new JSONObject();
			if (file!=null) {
				FileUtils.deleteQuietly(file);
				object.put("error", "");
			}else {
				object.put("error", "no such file");
			}
		}else {
			File file=FileUtils.getFile(new File("WebRoot/data/"+fileName));
			object=new JSONObject();
			if (file!=null) {
				try {
					FileUtils.forceDelete(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				object.put("error", "");
			}else {
				object.put("error", "no such file");
			}
		}
		
		return object.toString();
	}
	
	
	//通过工具转换csv
	public String toCSV(String pluginID,File file){
		JSONObject object=new JSONObject();
		try {
			if (file!=null) {
				String fileString=FileUtils.readFileToString(file);
				String csvString=Json2CSV.toCsv(fileString);
				System.out.println(pluginID);
				if (pluginID.equals("")) {
					File tmpFile=new File("WebRoot/data/"+file.getName()+".csv");
					FileUtils.writeStringToFile(tmpFile, csvString,"UTF-8");
					object.put("url", "data/"+tmpFile.getName());
					object.put("error", "");
				}else {
					File tmpFile=new File("WebRoot/data/"+pluginID+"/"+file.getName()+".csv");
					FileUtils.writeStringToFile(tmpFile, csvString,"UTF-8");
					object.put("url", "data/"+pluginID+"/"+tmpFile.getName());
					object.put("error", "");
				}
				return object.toString();
			}else {
				object.put("url", "");
				object.put("error", "no such file");
				return object.toString();
			}
		} catch (Exception e) {
			object.put("url", "");
			object.put("error", "transform failed");
			e.printStackTrace();
			return object.toString();
		}
	}
	//通过游标获取日志
	public String getLog(int currentCursor){
		if (LogQueueUtil.getInstance().getQueueSize()>0) {
			LogBean bean=LogQueueUtil.getInstance().getLog(currentCursor);
			JSONObject object=new JSONObject();
			object.put("lastCursor", bean.getCurrentCursor());
			object.put("log", bean.getLog());
			return object.toString();
		}
		return "";
	}
	
}
