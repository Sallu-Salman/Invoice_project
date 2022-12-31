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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }

        BufferedReader reader = request.getReader();
        Gson gson = new Gson();

        Item_json item_json = gson.fromJson(reader, Item_json.class);

        if(item_json.item_cost <= 0 || item_json.item_quantity < 0 || item_json.stock_rate <= 0 )
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid data Passed");
            return;
        }

        long generatedItemId = ItemMethods.createNewItem(item_json);

        if(generatedItemId == -1)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Item was not created");
        }

        else
        {
            item_json.item_id = generatedItemId;

            CommonMethods.responseObjectSender(response, "Item has been created", new Gson().toJson(item_json));
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
            Item_json[] item_jsons = ItemMethods.getItemsDetails();
            responseJson = new Gson().toJson(item_jsons);

            CommonMethods.responseArraySender(response, "success", responseJson);
        }
        else
        {
            long item_id = CommonMethods.parseId(request);

            Item_json item_json = ItemMethods.getItemDetails(item_id);

            if(item_json.item_name == null)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Invalid data passed");
                return;
            }

            responseJson = new Gson().toJson(item_json);

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



        long item_id = CommonMethods.parseId(request);

        if(!Filters.ifItemExists(item_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Item does not exists");
            return;
        }

        if(ItemMethods.deleteItem(item_id) == 0)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Item was not deleted");
        }
        else
        {
            CommonMethods.responseSender(response, "Item has been deleted");
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




        JSONObject jsonObject = CommonMethods.getBodyJson(request);

        Item_json item_json = Filters.checkAndLoadItem(jsonObject);

        item_json.item_id = CommonMethods.parseId(request);

        if(!Filters.ifItemExists(item_json.item_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Item does not exists");
            return;
        }

        if(ItemMethods.updateItem(item_json) == 0)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Item was not updated");
        }
        else
        {
            String responseJson = new Gson().toJson(item_json);

            CommonMethods.responseObjectSender(response, "Item has been updated", CommonMethods.refineJson(responseJson));
        }
    }

}
