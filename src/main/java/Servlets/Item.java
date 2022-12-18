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

@WebServlet("/api/items/*")
public class Item extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.emptyPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }

        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        ItemMethods itemMethods = new ItemMethods();

        Item_json item_json = gson.fromJson(reader, Item_json.class);

        if(item_json.item_cost <= 0 || item_json.item_quantity < 0 || item_json.stock_rate <= 0 )
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
        if(!(CommonMethods.emptyPath(request.getPathInfo()) || CommonMethods.paramPath(request.getPathInfo())))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }

        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();


        String responseJson;

        if (CommonMethods.emptyPath(request.getPathInfo()))
        {
            Item_json[] item_jsons = itemMethods.getItemsDetails();
            responseJson = new Gson().toJson(item_jsons);
        }
        else
        {
            long item_id = commonMethods.parseId(request);

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
        Filters filters = new Filters();

        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }

        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();

        long item_id = commonMethods.parseId(request);

        if(!filters.ifItemExists(item_id))
        {
            response.getWriter().println("Item does not exits");
            return;
        }

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

        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }

        ItemMethods itemMethods = new ItemMethods();
        CommonMethods commonMethods = new CommonMethods();
        Filters filters = new Filters();

        JSONObject jsonObject = commonMethods.getBodyJson(request);

        Item_json item_json = filters.checkAndLoadItem(jsonObject);

        item_json.item_id = commonMethods.parseId(request);

        if(!filters.ifItemExists(item_json.item_id))
        {
            response.getWriter().println("Item does not exists");
            return;
        }

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
