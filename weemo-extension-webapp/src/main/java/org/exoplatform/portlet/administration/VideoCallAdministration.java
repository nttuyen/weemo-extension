package org.exoplatform.portlet.administration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.logging.Logger;

import juzu.*;
import juzu.Response.Render;
import juzu.impl.common.JSON;
import juzu.plugin.ajax.Ajax;
import juzu.request.RenderContext;
import juzu.template.Template;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.portlet.PortletPreferences;
import org.exoplatform.services.organization.Group;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.model.videocall.VideoCallModel;
import org.exoplatform.portal.webui.page.PageIterator;
import org.exoplatform.services.jcr.ext.organization.GroupImpl;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.hibernate.GroupDAOImpl;
import org.exoplatform.services.organization.idm.ExtGroup;
import org.exoplatform.services.videocall.VideoCallService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.core.UIPageIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.internal.runners.model.EachTestNotifier;

public class VideoCallAdministration {

  @Inject
  @Path("index.gtmpl")
  Template index; 
  
  @Inject
  VideoCalls videoCalls;

  Logger log = Logger.getLogger("VideoCallAdministration");

  OrganizationService organizationService_;

  SpaceService spaceService_;
  
  VideoCallService videoCallService_;
  
  public static String USER_NAME = "userName";

  public static String LAST_NAME = "lastName";

  public static String FIRST_NAME = "firstName";
  
  public static String EMAIL = "email";

  @Inject
  Provider<PortletPreferences> providerPreferences;

  @Inject
  public VideoCallAdministration(OrganizationService organizationService, SpaceService spaceService, VideoCallService videoCallService)
  {
    organizationService_ = organizationService;
    spaceService_ = spaceService;
    videoCallService_ = videoCallService;
  }


  @View
  public void index(RenderContext renderContext) throws IOException
  {   
    String weemoKey = videoCallService_.getWeemoKey();
    boolean turnOffVideoCall = videoCallService_.isDisableVideoCall();
    index.with().set("turnOffVideoCall", turnOffVideoCall)
              .set("weemoKey", weemoKey)
              .render();
    videoCalls.setDisplaySuccessMsg(false);
  }  
  
  @Action
  @Route("/save")
  public Response save(VideoCallModel videoCallModel) {
     if(videoCallModel.getDisableVideoCall() == null) {
       videoCallModel.setDisableVideoCall("false");
     }
     VideoCallService videoCallService = new VideoCallService();
     videoCallService.saveVideoCallProfile(videoCallModel);
     videoCalls.setDisplaySuccessMsg(true);    
     return VideoCallAdministration_.index();
  }
  
  @Ajax
  @Resource
  public Response.Content openUserPermission() throws Exception {
    ObjectPageList objPageList = new ObjectPageList(organizationService_.getUserHandler().findUsers(new Query()).getAll(), 10);
    UIPageIterator uiIterator = new UIPageIterator();
    uiIterator.setPageList(objPageList);
    List<User> users = uiIterator.getCurrentPageData();
    JSONArray arrays = new JSONArray();
    for(int i=0; i< users.size(); i++) {
      User user = users.get(i);
      if(StringUtils.isEmpty(user.getDisplayName())) {
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
      }
      JSONObject obj = new JSONObject();
      obj.put("userName", user.getUserName());
      obj.put("firstName", user.getFirstName());
      obj.put("lastName", user.getLastName());
      obj.put("displayName", user.getDisplayName());
      obj.put("email", user.getEmail());
      arrays.put(obj.toString());
    }    
    return Response.ok(arrays.toString()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
    
  }
  
  @Ajax
  @Resource
  public Response.Content searchUserPermission(String keyword, String filter) throws Exception {
    Query q = new Query();
    if (keyword != null && (keyword = keyword.trim()).length() != 0) {
      if (keyword.indexOf("*") < 0) {
          if (keyword.charAt(0) != '*')
              keyword = "*" + keyword;
          if (keyword.charAt(keyword.length() - 1) != '*')
              keyword += "*";
      }
      keyword = keyword.replace('?', '_');
      if (USER_NAME.equals(filter)) {
          q.setUserName(keyword);
      }
      if (LAST_NAME.equals(filter)) {
          q.setLastName(keyword);
      }
      if (FIRST_NAME.equals(filter)) {
          q.setFirstName(keyword);
      }
      if (EMAIL.equals(filter)) {
          q.setEmail(keyword);
      }
    }
    
    List<User> users = organizationService_.getUserHandler().findUsers(q).getAll();
    JSONArray arrays = new JSONArray();
    for(int i=0; i< users.size(); i++) {
      User user = users.get(i);
      if(StringUtils.isEmpty(user.getDisplayName())) {
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
      }
      JSONObject obj = new JSONObject();
      obj.put("userName", user.getUserName());
      obj.put("firstName", user.getFirstName());
      obj.put("lastName", user.getLastName());
      obj.put("displayName", user.getDisplayName());
      obj.put("email", user.getEmail());
      arrays.put(obj.toString());
    }    
    return Response.ok(arrays.toString()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }  
  
  
  @Ajax
  @Resource
  public Response.Content openGroupPermission(String groupId) throws Exception {
    Collection<?> collection = organizationService_.getMembershipTypeHandler().findMembershipTypes();
    List<String> listMemberhip = new ArrayList<String>(5);
    StringBuffer sb = new StringBuffer();
    String memberships = "";
    for(Object obj : collection){
      listMemberhip.add(((MembershipType)obj).getName());      
    }
    if (!listMemberhip.contains("*")) listMemberhip.add("*");
    Collections.sort(listMemberhip);
    for (String string : listMemberhip) {
      sb.append(string).append(",");
    }
    memberships = sb.toString();
    memberships = memberships.substring(0, memberships.length()-1);
    
    String groups = "[";
    StringBuffer sbGroups = new StringBuffer();
    Collection<?> sibblingsGroup = null;
    if(StringUtils.isEmpty(groupId)) {
      sibblingsGroup = organizationService_.getGroupHandler().findGroups(null);      
    } else {
      Group group = organizationService_.getGroupHandler().findGroupById(groupId);
      String parentId = group.getParentId();
      Group parentGroup = null;
      if(parentId != null) {
        parentGroup = organizationService_.getGroupHandler().findGroupById(parentId);
      }
      sibblingsGroup = organizationService_.getGroupHandler().findGroups(parentGroup);      
    }
    if(sibblingsGroup != null && sibblingsGroup.size() > 0) { 
      for(Object obj : sibblingsGroup){      
        String groupLabel = ((ExtGroup)obj).getLabel();
        if(groupId != null && ((ExtGroup)obj).getId().equalsIgnoreCase(groupId)) {
          String groupObj = loadChildrenGroups(groupId, groupLabel);
          sbGroups.append(groupObj).append(",");
        } else {
          sb = new StringBuffer();
          sb.append("{\"group\":\""+((ExtGroup)obj).getId()+"\",\"label\":\""+groupLabel+"\"}");
          sbGroups.append(sb.toString()).append(",");
        }
      }
    }
    
    
    groups = groups.concat(sbGroups.toString());
    if(groups.length() > 1) {
      groups = groups.substring(0, groups.length()-1);
    }
    groups = groups.concat("]");
    StringBuffer sbResponse = new StringBuffer();
    sbResponse.append("{\"memberships\":\""+memberships+"\", \"groups\":"+groups+"}");
    return Response.ok(sbResponse.toString()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }
  
  public String loadChildrenGroups(String groupId, String groupLabel) throws Exception {
    JSONObject objGroup = new JSONObject();
    Group group = organizationService_.getGroupHandler().findGroupById(groupId);
    objGroup.put("group", groupId);
    objGroup.put("label", groupLabel);
    if(group != null) {
      Collection<?> collection = organizationService_.getGroupHandler().findGroups(group);
      if(collection.size() > 0) {
        StringBuffer sbChildren = new StringBuffer();
        sbChildren.append("[");
        for(Object obj : collection){          
          sbChildren.append("{\"group\":\""+((ExtGroup)obj).getId()+"\",\"label\":\""+((ExtGroup)obj).getLabel()+"\"},");
        } 
        String childrenGroups = sbChildren.toString();
        if(childrenGroups.length() > 1) {
          childrenGroups = childrenGroups.substring(0, childrenGroups.length()-1);
        }
        childrenGroups = childrenGroups.concat("]");
        objGroup.put("children", childrenGroups);
      }          
    }     
    
    
    /*List<String> visitedNodes = new ArrayList<String>();
    Map<String, String> maps = new HashedMap();
    Stack stack = new Stack();
    stack.push(groupName);
    visitedNodes.add(groupName);
    System.out.println(" == " + groupName);
    objGroup.put("group", groupName);
    objGroup.put("label", groupLabel);
    while(!stack.isEmpty()) {
      String groupId = (String) stack.peek();
      if(maps.get(groupId) != null) {
        groupId = maps.get(groupId) + "/" + groupId;
      }
      Group group = organizationService_.getGroupHandler().findGroupById("/" + groupId);
      if(group != null) {
        Collection<?> collection = organizationService_.getGroupHandler().findGroups(group);
        String unvisittedGroup = null;
        for(Object obj : collection) {          
          if(!visitedNodes.contains(((ExtGroup)obj).getGroupName())) {
            unvisittedGroup = ((ExtGroup)obj).getGroupName();
            maps.put(unvisittedGroup, groupId);
            break;          
          }
        }
        if(!StringUtils.isEmpty(unvisittedGroup)) {
          visitedNodes.add(unvisittedGroup);
          System.out.println("  ==== " + unvisittedGroup);
          objGroup.put("group", groupId);
          objGroup.put("label", groupLabel);
          stack.push(unvisittedGroup);
        } else {
          stack.pop();
        }
      } else stack.pop();
    }*/
    return objGroup.toString();
  }
  
}
