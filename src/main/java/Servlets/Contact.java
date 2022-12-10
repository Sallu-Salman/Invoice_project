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


@WebServlet("/contacts/*")
public class Contact extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        Filters filters = new Filters();
        ContactMethods contactMethods = new ContactMethods();

        Contact_json contact_json = gson.fromJson(reader, Contact_json.class);
        if(!filters.checkContact(contact_json))
        {
            response.getWriter().println("Invalid data passed !");
            return;
        }

        long generatedContactId = contactMethods.createNewContact(contact_json);

        if(generatedContactId == -1)
        {
            response.getWriter().println("Something went wrong !\nContact was not created.");
        }

        else
        {
            response.getWriter().println("Contact created successfully !");

            contact_json.contact_id = generatedContactId;

            String responseJson = new Gson().toJson(contact_json);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        ContactMethods contactMethods = new ContactMethods();
        CommonMethods commonMethods = new CommonMethods();

        long contact_id = commonMethods.parseId(request);
        String responseJson;

        if (contact_id == -1)
        {
            Contact_json[] contact_jsons = contactMethods.getContactsDetails();
            responseJson = new Gson().toJson(contact_jsons);
        }
        else
        {
            Contact_json contact_json = contactMethods.getContactDetails(contact_id);

            if(contact_json.contact_name == null)
            {
                response.getWriter().println("Invalid data passed !");
                return;
            }

            responseJson = new Gson().toJson(contact_json);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);

    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        ContactMethods contactMethods = new ContactMethods();
        CommonMethods commonMethods = new CommonMethods();

        long contact_id = commonMethods.parseId(request);

        if(contactMethods.deleteContact(contact_id) == 0)
        {
            response.getWriter().println("Something went wrong !\nContact was not deleted");
        }
        else
        {
            response.getWriter().println("Contact deleted successfully !");
        }

    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        ContactMethods contactMethods = new ContactMethods();
        CommonMethods commonMethods = new CommonMethods();

        long contact_id = commonMethods.parseId(request);

        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        Filters filters = new Filters();
        Contact_json contact_json = gson.fromJson(reader, Contact_json.class);

        if(!filters.checkContact(contact_json))
        {
            response.getWriter().println("Invalid data passed !");
            return;
        }

        contact_json.contact_id = contact_id;

        if(contactMethods.updateContact(contact_json) == 0)
        {
            response.getWriter().println("Something went wrong !\nContact was not updated");
        }
        else
        {
            response.getWriter().println("Contact updated successfully !");

            String responseJson = new Gson().toJson(contact_json);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        }
    }
}
