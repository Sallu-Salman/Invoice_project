package Servlets;

import Methods.CommonMethods;
import Methods.ContactMethods;
import Methods.Filters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.stream.Collectors;

import Templates.Contact_json;
import com.google.gson.JsonObject;
import sun.misc.IOUtils;


@WebServlet("/api/contacts/*")
public class Contact extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.emptyPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }

        BufferedReader reader = request.getReader();
        Gson gson = new Gson();


        Contact_json contact_json = gson.fromJson(reader, Contact_json.class);
        if(!Filters.checkContact(contact_json))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid data Passed");
            return;
        }

        long generatedContactId = ContactMethods.createNewContact(contact_json);

        if(generatedContactId == -1)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Contact was not created");
        }
        else
        {
            contact_json.contact_id = generatedContactId;
            CommonMethods.responseObjectSender(response, "Contact has been created", new Gson().toJson(contact_json));
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!(CommonMethods.emptyPath(request.getPathInfo()) || CommonMethods.paramPath(request.getPathInfo())))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }

        String responseJson;

        if (CommonMethods.emptyPath(request.getPathInfo()))
        {
            Contact_json[] contact_jsons = ContactMethods.getContactsDetails();
            responseJson = new Gson().toJson(contact_jsons);
            CommonMethods.responseArraySender(response, "success", responseJson);
        }
        else
        {
            long contact_id = CommonMethods.parseId(request);
            Contact_json contact_json = ContactMethods.getContactDetails(contact_id);

            if(contact_json.contact_name == null)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Invalid data Passed");
                return;
            }

            responseJson = new Gson().toJson(contact_json);
            CommonMethods.responseObjectSender(response, "success", responseJson);
        }

    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }




        long contact_id = CommonMethods.parseId(request);

        if(ContactMethods.deleteContact(contact_id) == 0)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Contact was not deleted");
        }
        else
        {
            CommonMethods.responseSender(response, "Contact has been deleted");
        }

    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }




        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        Contact_json contact_json = gson.fromJson(reader, Contact_json.class);

        if(!Filters.checkContact(contact_json))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid data Passed");
            return;
        }
        contact_json.contact_id = CommonMethods.parseId(request);

        if(ContactMethods.updateContact(contact_json) == 0)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Something went wrong. Contact was not updated");
        }
        else
        {
            CommonMethods.responseObjectSender(response, "Contact has been updated", new Gson().toJson(contact_json));
        }
    }
}
