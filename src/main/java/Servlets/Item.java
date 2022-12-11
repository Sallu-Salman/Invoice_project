package Servlets;

import Methods.CommonMethods;
import Methods.ContactMethods;
import Methods.Filters;
import Methods.ItemMethods;
import Templates.Contact_json;
import Templates.Item_json;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.HTTP;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/items/*")
public class Item extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        Filters filters = new Filters();
        ItemMethods itemMethods = new ItemMethods();

        Item_json item_json = gson.fromJson(reader, Item_json.class);

        if(!filters.checkItem(item_json))
        {
            response.getWriter().println("Invalid data passed !");
            return;
        }

        long generatedItemId = itemMethods.createNewItem(item_json);

        if(generatedItemId == -1)
        {
            response.getWriter().println("Something went wrong !\nItem was not created");
        }

        else
        {
            response.getWriter().println("Item created successfully");

            item_json.item_id = generatedItemId;

            String responseJson = new Gson().toJson(item_json);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();

        long item_id = commonMethods.parseId(request);
        String responseJson;

        if (item_id == -1)
        {
            Item_json[] item_jsons = itemMethods.getItemsDetails();
            responseJson = new Gson().toJson(item_jsons);
        }
        else
        {
            Item_json item_json = itemMethods.getItemDetails(item_id);

            if(item_json.item_name == null)
            {
                response.getWriter().println("Invalid data passed !");
                return;
            }

            responseJson = new Gson().toJson(item_json);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();

        long item_id = commonMethods.parseId(request);

        if(itemMethods.deleteItem(item_id) == 0)
        {
            response.getWriter().println("Something went wrong !\nItem was not deleted");
        }
        else
        {
            response.getWriter().println("Item deleted successfully !");
        }

    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();
        Filters filters = new Filters();

        JSONObject jsonObject = commonMethods.getBodyJson(request);

        Item_json item_json = filters.checkAndLoadItem(jsonObject);

        item_json.item_id = commonMethods.parseId(request);

        if(itemMethods.updateItem(item_json) == 0)
        {
            response.getWriter().println("Something went wrong !\nItem was not updated");
        }
        else
        {
            response.getWriter().println("Item updated successfully !");

            String responseJson = new Gson().toJson(item_json);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(commonMethods.refineJson(responseJson));
        }
    }

}
