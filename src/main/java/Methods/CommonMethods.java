package Methods;

import Templates.Item_json;
import Templates.Tax_json;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.SendEnhancedRequestBody;
import models.SendEnhancedResponseBody;
import models.SendRequestMessage;
import org.json.JSONException;
import org.json.JSONObject;
import services.Courier;
import services.SendService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CommonMethods
{

    static HashMap<Long, Tax_json> tax_details_hash = new HashMap<Long, Tax_json>();
    static HashMap<Long, ArrayList<Long>> group_tax_details_hash = new HashMap<Long, ArrayList<Long>>();

    public static Connection createConnection()
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/invoice_db","root","");
            return connection;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean conjunction(boolean key, StringBuilder query)
    {
        if(key)
        {
            key = !key;
        }
        else
        {
            query.append(" , ");
        }

        return key;
    }

    public static void responseSender(HttpServletResponse response, String message) throws IOException
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println(jsonObject);
    }

    public static void responseArraySender(HttpServletResponse response, String message, String content) throws IOException
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        jsonObject.add("content", new Gson().fromJson(content, JsonArray.class));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println(jsonObject);
    }

    public static void responseObjectSender(HttpServletResponse response, String message, String content) throws IOException
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        jsonObject.add("content", new Gson().fromJson(content, JsonObject.class));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println(jsonObject);
    }
    public static JSONObject readBodyJson(HttpServletRequest request)
    {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader;

        try
        {
            String line;
            reader =  request.getReader();
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line).append('\n');
            }

            reader.close();

            return new JSONObject(stringBuilder.toString());
        }
        catch (IOException e)
        {
            return null;
        }
        catch (JSONException e)
        {
            return null;
        }

    }
    public static boolean emptyPath(String path)
    {
        return (path == null || path.equals("/"));
    }

    public static boolean paramPath(String path)
    {
        return (path != null && path.matches("/\\d+/?"));
    }

    public static boolean invoiceFunctionPath(String path)
    {
        return path.matches("/\\d+/[a-z]+/?");
    }

    public static boolean invoiceStatusPath(String path)
    {
        return path.matches("/\\d+/status/[a-z]+/?");
    }
    public static long parseId(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();

        String[] splits = pathInfo.split("/");
        String modelId = splits[1];

        long id = Long.parseLong(modelId);

        return id;
    }


    public static String refineJson(String jsonString)
    {
        JSONObject jsonObject = new JSONObject(jsonString);

        if(jsonObject.getInt("item_quantity") < 0)
        {
            jsonObject.remove("item_quantity");
        }
        if(jsonObject.getFloat("item_cost") < 0)
        {
            jsonObject.remove("item_cost");
        }
        if(jsonObject.getFloat("stock_rate") < 0)
        {
            jsonObject.remove("stock_rate");
        }

        return jsonObject.toString();
    }

    public static int getTaxPercentage(long tax_id)
    {

        if(tax_details_hash.containsKey(tax_id))
        {
            Tax_json tax_json = tax_details_hash.get(tax_id);

            return tax_json.tax_percentage;
        }

        Connection connection = CommonMethods.createConnection();
        Tax_json tax_json = new Tax_json();


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT tax_name, tax_percentage, is_group FROM taxes WHERE tax_id = ?");
            statement.setLong(1, tax_id);

            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                tax_json.tax_name = set.getString("tax_name");
                tax_json.tax_percentage = set.getInt("tax_percentage");
                tax_json.is_group = set.getInt("is_group");
            }

            tax_details_hash.put(tax_id, tax_json);

            return tax_json.tax_percentage;

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static ArrayList<Long> getGroupTaxDetails(long tax_id)
    {
        if(group_tax_details_hash.containsKey(tax_id))
        {
            return group_tax_details_hash.get(tax_id);
        }

        Connection connection = CommonMethods.createConnection();
        ArrayList<Long> taxes = new ArrayList<Long>();

        try
        {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT tax_id FROM tax_groups WHERE tax_group_id = ?");
            preparedStatement.setLong(1, tax_id);

            ResultSet set = preparedStatement.executeQuery();

            while (set.next())
            {
                taxes.add(set.getLong("tax_id"));
            }

            group_tax_details_hash.put(tax_id, taxes);

            return taxes;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static String getTaxName(long tax_id)
    {
        if(tax_details_hash.containsKey(tax_id))
        {
            Tax_json tax_json = tax_details_hash.get(tax_id);

            return tax_json.tax_name;
        }

        Connection connection = CommonMethods.createConnection();
        Tax_json tax_json = new Tax_json();


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT tax_name, tax_percentage, is_group FROM taxes WHERE tax_id = ?");
            statement.setLong(1, tax_id);

            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                tax_json.tax_name = set.getString("tax_name");
                tax_json.tax_percentage = set.getInt("tax_percentage");
                tax_json.is_group = set.getInt("is_group");
            }

            tax_details_hash.put(tax_id, tax_json);

            return tax_json.tax_name;

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static int getTaxIsGroup(long tax_id)
    {
        if(tax_details_hash.containsKey(tax_id))
        {
            Tax_json tax_json = tax_details_hash.get(tax_id);

            return tax_json.is_group;
        }

        Connection connection = CommonMethods.createConnection();
        Tax_json tax_json = new Tax_json();


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT tax_name, tax_percentage, is_group FROM taxes WHERE tax_id = ?");
            statement.setLong(1, tax_id);

            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                tax_json.tax_name = set.getString("tax_name");
                tax_json.tax_percentage = set.getInt("tax_percentage");
                tax_json.is_group = set.getInt("is_group");
            }

            tax_details_hash.put(tax_id, tax_json);

            return tax_json.is_group;

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendEmail(long invoice_id, String customer_name, String customer_email, int total_cost, String invoice_date)
    {
        Courier.init("pk_prod_2HQDMJRGV8M9Z2HQA1ABRBDTMQ3N");

        SendEnhancedRequestBody requestBody = new SendEnhancedRequestBody();
        SendRequestMessage sendRequestMessage = new SendRequestMessage();

        HashMap<String, String> to = new HashMap<String, String>();
        to.put("email", customer_email);
        sendRequestMessage.setTo(to);
        sendRequestMessage.setTemplate("TGJENRVNNK4ZMGH5FW94MPCPM8JK");


        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("invoice_id", invoice_id);
        data.put("invoice_amount", total_cost);
        data.put("invoice_date", invoice_date);
        data.put("customer_name", customer_name);
        sendRequestMessage.setData(data);

        requestBody.setMessage(sendRequestMessage);
        try
        {
            SendEnhancedResponseBody responseBody = new SendService().sendEnhancedMessage(requestBody);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
