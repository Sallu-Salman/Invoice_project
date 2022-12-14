package Methods;

import Templates.Contact_json;
import Templates.Invoice_json;
import Templates.Item_json;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InvoiceMethods
{
    public int findSubTotal(Item_json[] line_items)
    {
        int subTotal = 0;

        for(Item_json item_json : line_items)
        {
            subTotal += ( item_json.item_cost * item_json.item_quantity);
        }

        return subTotal;
    }

    public Item_json[] fetchLineItemsDetails(Item_json[] line_items)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        StringBuilder query = new StringBuilder("SELECT item_id, item_name, item_cost FROM items WHERE item_id IN ( ");
        boolean key = true;
        ResultSet set;

        HashMap<Long, ArrayList<Integer>> itemIdIndexMapper = new HashMap<Long, ArrayList<Integer>>();

        for(int i=0; i< line_items.length; i++)
        {
            key = commonMethods.conjunction(key, query);
            query.append(line_items[i].item_id);
            ArrayList<Integer> itemIdList;

            if(itemIdIndexMapper.containsKey(line_items[i].item_id))
            {
                itemIdList = itemIdIndexMapper.get(line_items[i].item_id);
                itemIdIndexMapper.remove(line_items[i].item_id);
            }
            else
            {
                itemIdList = new ArrayList<Integer>();
            }

            itemIdList.add(i);
            itemIdIndexMapper.put(line_items[i].item_id, itemIdList);
        }

        query.append(" );");


        try
        {
            Statement statement = connection.createStatement();
            set = statement.executeQuery(query.toString());

            while (set.next())
            {
                ArrayList<Integer> indices = itemIdIndexMapper.get(set.getLong("item_id"));

                for(int index : indices)
                {
                    line_items[index].item_name = set.getString("item_name");

                    if(line_items[index].item_cost == 0)
                    {
                        line_items[index].item_cost = set.getInt("item_cost");
                    }
                }

            }

            connection.close();

        }
        catch(SQLException e)
        {
            return null;
        }

        return line_items;
    }

    public String buildInvoiceQuery(Invoice_json invoice_json)
    {
        StringBuilder query = new StringBuilder("INSERT INTO invoices ( customer_id, invoice_date, ");
        StringBuilder values = new StringBuilder();

        invoice_json.total_cost = invoice_json.sub_total;

        if(invoice_json.salesperson_id != 0)
        {
            query.append(" salesperson_id, ");
            values.append(invoice_json.salesperson_id + " , ");
        }
        if(invoice_json.subject != null)
        {
            query.append(" subject, ");
            values.append(" '" + invoice_json.subject + "' , ");
        }
        if(invoice_json.terms_and_conditions != null)
        {
            query.append(" terms_and_conditions, ");
            values.append(" '" + invoice_json.terms_and_conditions + "' , ");
        }
        if(invoice_json.customer_notes != null)
        {
            query.append(" customer_notes, ");
            values.append(" '" + invoice_json.customer_notes + "' , ");
        }
        if(invoice_json.tax != 0)
        {
            query.append(" tax, ");
            values.append(invoice_json.tax + " , ");
            invoice_json.total_cost += invoice_json.tax;
        }
        if(invoice_json.discount != 0)
        {
            query.append(" discount, ");
            values.append(invoice_json.discount + " , ");
            invoice_json.total_cost -= invoice_json.discount;
        }
        if(invoice_json.charges != 0)
        {
            query.append(" charges, ");
            values.append(invoice_json.charges + " , ");
            invoice_json.total_cost += invoice_json.charges;
        }

        query.append(" sub_total, total_cost ) VALUES ( "+invoice_json.customer_id + ", CURDATE() , ");
        query.append(values);
        query.append(invoice_json.sub_total + " , " + invoice_json.total_cost + " );");

        return query.toString();
    }

    public boolean storeLineItems(Item_json[] lineItems, long invoice_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();

        StringBuilder query = new StringBuilder("INSERT INTO line_items (invoice_id, item_id, item_name, item_cost, item_quantity) VALUES ");
        boolean key = true;

        for(Item_json lineItem : lineItems)
        {
            if(lineItem.item_name != null)
            {
                key = commonMethods.conjunction(key, query);

                StringBuilder values = new StringBuilder();

                values.append(" ( ");
                values.append(invoice_id);
                values.append(" , ");
                values.append(lineItem.item_id);
                values.append(" , ");
                values.append(" '"+lineItem.item_name+"' ");
                values.append(" , ");
                values.append(lineItem.item_cost);
                values.append(" , ");
                values.append(lineItem.item_quantity);
                values.append(" ) ");

                query.append(values);
            }
        }

        query.append(" ; ");

        try
        {
            PreparedStatement statement = connection.prepareStatement(query.toString());
            int affected_rows = statement.executeUpdate();
            connection.close();

            return (affected_rows > 0);
        }
        catch(SQLException e)
        {
            return false;
        }
    }

    public long raiseInvoice(Invoice_json invoice_json)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();
        InvoiceMethods invoiceMethods = new InvoiceMethods();

        String query = invoiceMethods.buildInvoiceQuery(invoice_json);

        try
        {
            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();

            if(set.next())
            {
                invoice_json.invoice_id =  set.getLong(1);
            }

            connection.close();

            return invoice_json.invoice_id;
        }
        catch (SQLException e)
        {
            return -1;
        }
    }

    public int deleteInvoice(long invoice_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM invoices WHERE invoice_id = ?");
            statement.setLong(1, invoice_id);
            int affected_rows = statement.executeUpdate();

            connection.close();
            return affected_rows;
        }
        catch (SQLException e)
        {
            return 0;
        }
    }

    public String getInvoicesDetails()
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        JsonArray jsonArray = new JsonArray();

        try
        {
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("SELECT invoices.invoice_id, invoices.invoice_date , contacts.contact_name, invoices.status , invoices.total_cost FROM invoices INNER JOIN contacts on invoices.customer_id = contacts.contact_id ;");

            while (set.next())
            {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("invoice_id", set.getLong("invoice_id"));
                jsonObject.addProperty("invoice_date", set.getString("invoice_date"));
                jsonObject.addProperty("customer_name", set.getString("contact_name"));
                jsonObject.addProperty("status", set.getString("status"));
                jsonObject.addProperty("amount", set.getInt("total_cost"));

                jsonArray.add(jsonObject);
            }

            return jsonArray.toString();

        }
        catch (SQLException e)
        {
            return null;
        }
    }

    public JSONObject readBodyJson(HttpServletRequest request)
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

    public int updateLineItems(JSONArray newLineItems, long invoice_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();
        InvoiceMethods invoiceMethods = new InvoiceMethods();
        Gson gson = new Gson();
        int sub_total = 0;


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT line_item_id, item_cost, item_quantity FROM line_items WHERE invoice_id = ?;");
            statement.setLong(1, invoice_id);
            ResultSet rs = statement.executeQuery();

            HashMap<Long, JSONObject> existingLineItems = new HashMap<Long, JSONObject>();
            List<Item_json> addLineItemList = new ArrayList<Item_json>();


            while(rs.next())
            {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("item_cost", rs.getInt("item_cost"));
                jsonObject.put("item_quantity", rs.getInt("item_quantity"));

                sub_total += (rs.getInt("item_cost") * rs.getInt("item_quantity"));

                existingLineItems.put(rs.getLong("line_item_id"), jsonObject);
            }

            for(int i = 0; i<newLineItems.length(); i++)
            {
                JSONObject jsonObject = newLineItems.getJSONObject(i);

                if(!jsonObject.has("line_item_id"))
                {
                    //NO ID so new Insert
                    addLineItemList.add(gson.fromJson(jsonObject.toString(), Item_json.class));
                    sub_total += (jsonObject.getInt("item_cost") * jsonObject.getInt("item_quantity"));
                }
                else
                {
                    JSONObject existingObject = existingLineItems.get(jsonObject.getLong("line_item_id"));

                    if(jsonObject.getInt("item_cost") != existingObject.getInt("item_cost") || jsonObject.getInt("item_quantity") != existingObject.getInt("item_quantity"))
                    {
                        StringBuilder updateLineItemQuery = new StringBuilder("UPDATE line_items SET ");
                        boolean key = true;

                        if(jsonObject.getInt("item_cost") != existingObject.getInt("item_cost"))
                        {
                            key = commonMethods.conjunction(key, updateLineItemQuery);
                            updateLineItemQuery.append("item_cost = "+jsonObject.getInt("item_cost"));
                        }
                        if(jsonObject.getInt("item_quantity") != existingObject.getInt("item_quantity"))
                        {
                            commonMethods.conjunction(key, updateLineItemQuery);
                            updateLineItemQuery.append("item_quantity = "+jsonObject.getInt("item_quantity"));
                        }

                        updateLineItemQuery.append(" WHERE line_item_id ="+jsonObject.getLong("line_item_id"));

                        statement = connection.prepareStatement(updateLineItemQuery.toString());
                        statement.executeUpdate();

                        sub_total -= (existingObject.getInt("item_cost") * existingObject.getInt("item_quantity"));
                        sub_total += (jsonObject.getInt("item_cost") * jsonObject.getInt("item_quantity"));

                    }

                    existingLineItems.remove(jsonObject.getLong("line_item_id"));
                }
            }

            if(!existingLineItems.isEmpty())
            {
                StringBuilder deleteLineItems = new StringBuilder("DELETE FROM line_items WHERE line_item_id IN ( ");
                boolean key = true;

                for(long removedLineItemId : existingLineItems.keySet())
                {
                    key = commonMethods.conjunction(key, deleteLineItems);
                    deleteLineItems.append(removedLineItemId);

                    JSONObject existingObject = existingLineItems.get(removedLineItemId);

                    sub_total -= (existingObject.getInt("item_cost") * existingObject.getInt("item_quantity"));

                }

                deleteLineItems.append(");");

                statement = connection.prepareStatement(deleteLineItems.toString());
                statement.executeUpdate();
            }

            if(addLineItemList.size() > 0)
            {
                invoiceMethods.storeLineItems(invoiceMethods.fetchLineItemsDetails(addLineItemList.toArray(new Item_json[addLineItemList.size()])), invoice_id);
            }

            connection.close();

        }
        catch (SQLException e)
        {
            return 0;
        }
        catch (JSONException e)
        {
            return 0;
        }

        return sub_total;
    }

    public int updateInvoice(JSONObject inputJson)
    {
        CommonMethods commonMethods = new CommonMethods();
        InvoiceMethods invoiceMethods = new InvoiceMethods();
        Connection connection = commonMethods.createConnection();
        Gson gson = new Gson();

        boolean isTotalChanged = false;

        if(inputJson.has("line_items"))
        {
            int sub_total = invoiceMethods.findSubTotal(gson.fromJson(inputJson.getJSONArray("line_items").toString(), Item_json[].class));

            if(sub_total == 0)
            {
                return 0;
            }
            inputJson.put("sub_total", sub_total);
            isTotalChanged = true;
        }

        StringBuilder invoiceUpdateQuery = new StringBuilder("UPDATE invoices SET ");
        boolean key = true;

        if(inputJson.has("customer_id"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("customer_id = " + inputJson.getLong("cusotmer_id"));
        }
        if(inputJson.has("salesperson_id"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("salesperson_id = " + inputJson.getLong("salesperson_id"));
        }
        if(inputJson.has("subject"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("subject = '"+inputJson.getString("subject") + "' ");
        }
        if(inputJson.has("invoice_date"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("invoice_date = '"+inputJson.getString("invoice_date") + "' ");
        }
        if(inputJson.has("terms_and_conditions"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("terms_and_conditions = '"+inputJson.getString("terms_and_conditions") + "' ");
        }
        if(inputJson.has("customer_notes"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("customer_notes = '"+inputJson.getString("customer_notes") + "' ");
        }
        if(inputJson.has("sub_total"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("sub_total = " + inputJson.getInt("sub_total"));
        }
        if(inputJson.has("tax"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("tax = " + inputJson.getInt("tax"));
            isTotalChanged = true;
        }
        if(inputJson.has("discount"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("discount = " + inputJson.getInt("discount"));
            isTotalChanged = true;
        }
        if(inputJson.has("charges"))
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("charges = " + inputJson.getInt("charges"));
            isTotalChanged = true;
        }
        if(isTotalChanged)
        {
            key = commonMethods.conjunction(key, invoiceUpdateQuery);
            invoiceUpdateQuery.append("total_cost = sub_total + tax - discount + charges ");
        }

        invoiceUpdateQuery.append(" WHERE invoice_id = " + inputJson.getLong("invoice_id") + ";");

        try
        {
            PreparedStatement statement = connection.prepareStatement(invoiceUpdateQuery.toString());
            int affected_rows = statement.executeUpdate();

            if(inputJson.has("line_items"))
            {
                int newSubTotal = invoiceMethods.updateLineItems(inputJson.getJSONArray("line_items"), inputJson.getLong("invoice_id"));

                if(newSubTotal == 0)
                {
                    return 0;
                }

            }


            connection.close();

            return affected_rows;
        }
        catch(SQLException e)
        {
            return 0;
        }

    }

    public String getInvoiceDetails(long invoice_id)
    {
        CommonMethods commonMethods = new CommonMethods();

        Connection connection =  commonMethods.createConnection();
        int totalLineItems = 0;

        JsonObject responseJson = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT invoices.invoice_id , contacts.contact_name as customer_name, invoices.invoice_date , salespersons.salesperson_name , invoices.subject , invoices.terms_and_conditions , invoices.customer_notes , invoices.sub_total , invoices.tax , invoices.discount , invoices.charges , invoices.total_cost, invoices.status , line_items.line_item_id ,line_items.item_id , line_items.item_name , line_items.item_cost , line_items.item_quantity FROM invoices INNER JOIN contacts ON invoices.customer_id = contacts.contact_id INNER JOIN salespersons ON invoices.salesperson_id = salespersons.salesperson_id INNER JOIN line_items ON invoices.invoice_id = line_items.invoice_id  WHERE invoices.invoice_id = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            statement.setLong(1, invoice_id);

            ResultSet rs = statement.executeQuery();

            for(int i=0; rs.next(); i++)
            {
                if(i == 0)
                {
                    responseJson.addProperty("invoice_id", rs.getLong("invoice_id"));
                    responseJson.addProperty("status", rs.getString("status"));
                    responseJson.addProperty("invoice_date", rs.getString("invoice_date"));
                    responseJson.addProperty("customer_name", rs.getString("customer_name"));

                    if(rs.getString("salesperson_name") != null)
                    {
                        responseJson.addProperty("salesperson_name", rs.getString("salesperson_name"));
                    }
                    if(rs.getString("subject") != null)
                    {
                        responseJson.addProperty("subject", rs.getString("subject"));
                    }
                }

                JsonObject lineItemObject = new JsonObject();

                lineItemObject.addProperty("line_item_id" , rs.getLong("line_item_id"));
                lineItemObject.addProperty("item_id", rs.getLong("item_id"));
                lineItemObject.addProperty("item_name", rs.getString("item_name"));
                lineItemObject.addProperty("item_cost", rs.getInt("item_cost"));
                lineItemObject.addProperty("item_quantity", rs.getInt("item_quantity"));
                lineItemObject.addProperty("amount", (rs.getInt("item_cost") * rs.getInt("item_quantity")));

                jsonArray.add(lineItemObject);

            }

            responseJson.add("line_items", jsonArray);

            rs.previous();

            responseJson.addProperty("sub_total", rs.getInt("sub_total"));

            if(rs.getInt("tax") != 0)
            {
                responseJson.addProperty("tax", rs.getInt("tax"));
            }
            if(rs.getInt("discount") != 0)
            {
                responseJson.addProperty("discount", rs.getInt("discount"));
            }
            if(rs.getInt("charges") != 0)
            {
                responseJson.addProperty("charges", rs.getInt("charges"));
            }

            responseJson.addProperty("total_cost", rs.getInt("total_cost"));

            if(rs.getString("customer_notes") != null)
            {
                responseJson.addProperty("customer_notes", rs.getString("customer_notes"));
            }
            if(rs.getString("terms_and_conditions") != null)
            {
                responseJson.addProperty("terms_and_conditions", rs.getString("terms_and_conditions"));
            }



            connection.close();

            return responseJson.toString();
        }
        catch (SQLException e)
        {
            return null;
        }
    }

}
