package Methods;

import Templates.Contact_json;
import Templates.Invoice_json;
import Templates.Item_json;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysql.cj.jdbc.ConnectionImpl;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InvoiceMethods
{
    public static float findSubTotal(Item_json[] line_items)
    {
        float subTotal = 0;

        for(Item_json item_json : line_items)
        {
            subTotal += ( item_json.item_cost * item_json.item_quantity);
        }

        return subTotal;
    }

    public static Item_json[] fetchLineItemsDetails(Item_json[] line_items)
    {

        Connection connection = CommonMethods.createConnection();

        StringBuilder query = new StringBuilder("SELECT item_id, item_name, item_cost, stock_rate FROM items WHERE item_id IN ( ");
        boolean key = true;
        ResultSet set;

        HashMap<Long, ArrayList<Integer>> itemIdIndexMapper = new HashMap<Long, ArrayList<Integer>>();

        for(int i=0; i< line_items.length; i++)
        {
            key = CommonMethods.conjunction(key, query);
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
                        line_items[index].item_cost = set.getFloat("item_cost");
                    }

                    line_items[index].stock_rate = set.getFloat("stock_rate");
                }

                itemIdIndexMapper.remove(set.getLong("item_id"));

            }

            if(!itemIdIndexMapper.isEmpty())
            {
                return null;
            }

            connection.close();

        }
        catch(SQLException e)
        {
            return null;
        }

        return line_items;
    }

    public static String buildInvoiceQuery(Invoice_json invoice_json)
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

    public static boolean storeLineItems(Item_json[] lineItems, long invoice_id)
    {

        Connection connection =  CommonMethods.createConnection();

        StringBuilder query = new StringBuilder("INSERT INTO line_items (invoice_id, item_id, item_name, item_cost, item_quantity, stock_rate) VALUES ");
        boolean key = true;

        for(Item_json lineItem : lineItems)
        {
            if(lineItem.item_name != null)
            {
                key = CommonMethods.conjunction(key, query);

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
                values.append(" , ");
                values.append(lineItem.stock_rate);
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

    public static long raiseInvoice(Invoice_json invoice_json)
    {

        Connection connection =  CommonMethods.createConnection();


        if(!Filters.ifCustomerExists(invoice_json.customer_id))
        {
            return -1;
        }
        if(invoice_json.salesperson_id != 0 && !Filters.ifSalespersonExists(invoice_json.salesperson_id))
        {
            return -1;
        }

        String query = InvoiceMethods.buildInvoiceQuery(invoice_json);

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

    public static int deleteInvoice(long invoice_id)
    {

        Connection connection =  CommonMethods.createConnection();

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

    public static String getInvoicesDetails()
    {

        Connection connection = CommonMethods.createConnection();

        JsonArray jsonArray = new JsonArray();

        try
        {
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("SELECT invoices.invoice_id, invoices.invoice_date , contacts.contact_name, invoices.status , invoices.total_cost, invoices.payment_made, invoices.written_off_amount FROM invoices INNER JOIN contacts on invoices.customer_id = contacts.contact_id ;");

            while (set.next())
            {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("invoice_id", set.getLong("invoice_id"));
                jsonObject.addProperty("invoice_date", set.getString("invoice_date"));
                jsonObject.addProperty("customer_name", set.getString("contact_name"));
                jsonObject.addProperty("status", set.getString("status"));
                jsonObject.addProperty("amount", set.getFloat("total_cost"));
                jsonObject.addProperty("balance_due", (set.getFloat("total_cost") - set.getFloat("payment_made") - set.getFloat("written_off_amount")));

                jsonArray.add(jsonObject);
            }

            return jsonArray.toString();

        }
        catch (SQLException e)
        {
            return null;
        }
    }

    public static void updateSentInvoiceItems(HashMap<Long,Integer> item_id_hash)
    {

        Connection connection = CommonMethods.createConnection();

        try
        {

            StringBuilder itemQuery = new StringBuilder("UPDATE items SET item_quantity = CASE item_id ");
            StringBuilder reducedItemIds = new StringBuilder();
            boolean reducedIdKey = true;

            for(long item_id : item_id_hash.keySet())
            {
                itemQuery.append(" WHEN " + item_id + " THEN item_quantity + " + item_id_hash.get(item_id));
                reducedIdKey = CommonMethods.conjunction(reducedIdKey, reducedItemIds);
                reducedItemIds.append(item_id);
            }

            itemQuery.append(" ELSE item_quantity END WHERE item_id IN ( " + reducedItemIds+" );");

            PreparedStatement statement = connection.prepareStatement(itemQuery.toString());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {

        }
    }

    public static float updateLineItems(JSONArray newLineItems, long invoice_id, String invoice_status)
    {

        Connection connection = CommonMethods.createConnection();

        Gson gson = new Gson();
        float sub_total = 0;
        float change_in_inventory_asset = 0;

        if(!Filters.ifItemsExists(newLineItems))
        {
            return 0;
        }

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT line_item_id, item_id, item_cost, item_quantity, stock_rate FROM line_items WHERE invoice_id = ?;");
            statement.setLong(1, invoice_id);
            ResultSet rs = statement.executeQuery();

            HashMap<Long, JSONObject> existingLineItems = new HashMap<Long, JSONObject>();
            HashMap<Long, Integer> updateItemHash = new HashMap<Long, Integer>();
            List<Item_json> addLineItemList = new ArrayList<Item_json>();



            while(rs.next())
            {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("item_cost", rs.getFloat("item_cost"));
                jsonObject.put("item_quantity", rs.getInt("item_quantity"));
                jsonObject.put("stock_rate", rs.getFloat("stock_rate"));
                jsonObject.put("item_id", rs.getLong("item_id"));

                sub_total += (rs.getFloat("item_cost") * rs.getInt("item_quantity"));

                existingLineItems.put(rs.getLong("line_item_id"), jsonObject);
            }

            for(int i = 0; i<newLineItems.length(); i++)
            {
                JSONObject jsonObject = newLineItems.getJSONObject(i);

                if(!jsonObject.has("line_item_id"))
                {
                    //NO ID so new Insert
                    addLineItemList.add(gson.fromJson(jsonObject.toString(), Item_json.class));
                }
                else
                {
                    JSONObject existingObject = existingLineItems.get(jsonObject.getLong("line_item_id"));

                    if(jsonObject.getFloat("item_cost") != existingObject.getFloat("item_cost") || jsonObject.getInt("item_quantity") != existingObject.getInt("item_quantity"))
                    {
                        StringBuilder updateLineItemQuery = new StringBuilder("UPDATE line_items SET ");
                        boolean key = true;

                        if(jsonObject.getFloat("item_cost") != existingObject.getFloat("item_cost"))
                        {
                            key = CommonMethods.conjunction(key, updateLineItemQuery);
                            updateLineItemQuery.append("item_cost = "+jsonObject.getFloat("item_cost"));
                        }
                        if(jsonObject.getInt("item_quantity") != existingObject.getInt("item_quantity"))
                        {
                            CommonMethods.conjunction(key, updateLineItemQuery);
                            updateLineItemQuery.append("item_quantity = "+jsonObject.getInt("item_quantity"));

                            if(invoice_status.equals("SENT"))
                            {
                                int item_quantity =(jsonObject.getInt("item_quantity") - existingObject.getInt("item_quantity"));

                                change_in_inventory_asset += (item_quantity * existingObject.getFloat("stock_rate"));

                                item_quantity *= -1;

                                if(updateItemHash.containsKey(existingObject.getLong("item_id")))
                                {
                                    item_quantity += updateItemHash.get(existingObject.getLong("item_id"));
                                    updateItemHash.remove(existingObject.getLong("item_id"));
                                }

                                updateItemHash.put(existingObject.getLong("item_id"), item_quantity);


                            }

                        }

                        updateLineItemQuery.append(" WHERE line_item_id ="+jsonObject.getLong("line_item_id"));

                        statement = connection.prepareStatement(updateLineItemQuery.toString());
                        statement.executeUpdate();

                        sub_total -= (existingObject.getFloat("item_cost") * existingObject.getInt("item_quantity"));
                        sub_total += (jsonObject.getFloat("item_cost") * jsonObject.getInt("item_quantity"));

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
                    key = CommonMethods.conjunction(key, deleteLineItems);
                    deleteLineItems.append(removedLineItemId);

                    JSONObject existingObject = existingLineItems.get(removedLineItemId);

                    sub_total -= (existingObject.getFloat("item_cost") * existingObject.getInt("item_quantity"));

                    if(invoice_status.equals("SENT"))
                    {
                        int item_quantity = existingObject.getInt("item_quantity");

                        change_in_inventory_asset -= (item_quantity * existingObject.getFloat("stock_rate"));

                        if(updateItemHash.containsKey(existingObject.getLong("item_id")))
                        {
                            item_quantity += updateItemHash.get(existingObject.getLong("item_id"));
                            updateItemHash.remove(existingObject.getLong("item_id"));
                        }

                        updateItemHash.put(existingObject.getLong("item_id"), item_quantity);
                    }

                }

                deleteLineItems.append(");");

                statement = connection.prepareStatement(deleteLineItems.toString());
                statement.executeUpdate();

            }

            if(addLineItemList.size() > 0)
            {
                Item_json[] insertingLineItems = InvoiceMethods.fetchLineItemsDetails(addLineItemList.toArray(new Item_json[addLineItemList.size()]));
                InvoiceMethods.storeLineItems(insertingLineItems, invoice_id);
                sub_total += InvoiceMethods.findSubTotal(insertingLineItems);

                if(invoice_status.equals("SENT"))
                {
                    //Reduce the item quantity in items table

                    for(Item_json item_json : insertingLineItems)
                    {
                        change_in_inventory_asset += (item_json.item_quantity * item_json.stock_rate);

                        if(updateItemHash.containsKey(item_json.item_id))
                        {
                            item_json.item_quantity += updateItemHash.get(item_json.item_id);
                            updateItemHash.remove(item_json.item_id);
                        }

                        updateItemHash.put(item_json.item_id, (item_json.item_quantity )*(-1));
                    }

                }

            }

            if(updateItemHash.size() > 0)
            {
                InvoiceMethods.updateSentInvoiceItems(updateItemHash);
            }
            if(change_in_inventory_asset != 0)
            {
                statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Inventory asset';");
                statement.setFloat(1, change_in_inventory_asset);
                statement.executeUpdate();

                statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Cost of Goods Sold';");
                statement.setFloat(1, change_in_inventory_asset);
                statement.executeUpdate();
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
        catch (Exception e )
        {
            return 0;
        }

        return sub_total;
    }

    public static int updateInvoice(JSONObject inputJson, String invoice_status)
    {


        Connection connection = CommonMethods.createConnection();

        boolean isTotalChanged = false;
        float old_total_cost = 0;

        try
        {

            StringBuilder invoiceUpdateQuery = new StringBuilder("UPDATE invoices SET ");
            boolean key = true;

            if(inputJson.has("customer_id"))
            {
                if(!Filters.ifCustomerExists(inputJson.getLong("customer_id")))
                {
                    return 0;
                }

                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("customer_id = " + inputJson.getLong("customer_id"));
            }
            if(inputJson.has("salesperson_id"))
            {
                if(!Filters.ifSalespersonExists(inputJson.getLong("salesperson_id")))
                {
                    return 0;
                }
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("salesperson_id = " + inputJson.getLong("salesperson_id"));
            }

            if(inputJson.has("subject"))
            {
                if(!Filters.checkSubject(inputJson.getString("subject")))
                {
                    return 0;
                }
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("subject = '"+inputJson.getString("subject") + "' ");
            }
            if(inputJson.has("invoice_date")) {
                if (!GenericValidator.isDate(inputJson.getString("invoice_date"), "yyyy-MM-dd", true))
                {
                    return 0;
                }
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("invoice_date = '"+inputJson.getString("invoice_date") + "' ");
            }
            if(inputJson.has("terms_and_conditions"))
            {
                if(!Filters.checkTermsAndConditions(inputJson.getString("terms_and_conditions")))
                {
                    return 0;
                }
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("terms_and_conditions = '"+inputJson.getString("terms_and_conditions") + "' ");
            }
            if(inputJson.has("customer_notes"))
            {
                if(!Filters.checkCustomerNotes(inputJson.getString("customer_notes")))
                {
                    return 0;
                }
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("customer_notes = '"+inputJson.getString("customer_notes") + "' ");
            }
            if(inputJson.has("line_items"))
            {
                float newSubTotal = InvoiceMethods.updateLineItems(inputJson.getJSONArray("line_items"), inputJson.getLong("invoice_id"), invoice_status);

                if(newSubTotal == 0)
                {
                    return 0;
                }

                inputJson.put("sub_total", newSubTotal);
                isTotalChanged = true;
            }
            if(inputJson.has("sub_total"))
            {
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("sub_total = " + inputJson.getFloat("sub_total"));
            }
            if(inputJson.has("tax"))
            {
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("tax = " + inputJson.getFloat("tax"));
                isTotalChanged = true;
            }
            if(inputJson.has("discount"))
            {
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("discount = " + inputJson.getFloat("discount"));
                isTotalChanged = true;
            }
            if(inputJson.has("charges"))
            {
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("charges = " + inputJson.getFloat("charges"));
                isTotalChanged = true;
            }
            if(isTotalChanged)
            {
                key = CommonMethods.conjunction(key, invoiceUpdateQuery);
                invoiceUpdateQuery.append("total_cost = sub_total + tax - discount + charges ");

                if(invoice_status.equals("SENT"))
                {
                    PreparedStatement statement = connection.prepareStatement("SELECT total_cost FROM invoices WHERE invoice_id = ?;");
                    statement.setLong(1, inputJson.getLong("invoice_id"));

                    ResultSet set = statement.executeQuery();

                    while(set.next())
                    {
                        old_total_cost = set.getFloat("total_cost");
                    }
                }
            }

            invoiceUpdateQuery.append(" WHERE invoice_id = " + inputJson.getLong("invoice_id") + ";");

            if(key)
            {
                return 0;
            }

            PreparedStatement statement = connection.prepareStatement(invoiceUpdateQuery.toString());
            int affected_rows = statement.executeUpdate();

            if(invoice_status.equals("SENT"))
            {
                statement = connection.prepareStatement("SELECT total_cost FROM invoices WHERE invoice_id = ?;");
                statement.setLong(1, inputJson.getLong("invoice_id"));

                ResultSet set = statement.executeQuery();

                while(set.next())
                {
                    InvoiceMethods.updateSalesAccount(old_total_cost, set.getFloat("total_cost"));
                }
            }

            connection.close();

            return affected_rows;
        }
        catch(SQLException e)
        {
            return 0;
        }
        catch(JSONException e)
        {
            return 0;
        }

    }

    public static void updateSalesAccount(float old_total, float new_total)
    {

        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? + ? WHERE account_name = 'Sales';");
            statement.setFloat(1, old_total);
            statement.setFloat(2, new_total);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? + ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, old_total);
            statement.setFloat(2, new_total);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {

        }
    }

    public static String getInvoiceDetails(long invoice_id)
    {


        Connection connection =  CommonMethods.createConnection();

        JsonObject responseJson = new JsonObject();
        JsonArray jsonArray = new JsonArray();


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT invoices.invoice_id , contacts.contact_name as customer_name, invoices.invoice_date , (SELECT salesperson_name FROM salespersons WHERE salesperson_id = invoices.salesperson_id) AS salesperson_name , invoices.subject , invoices.terms_and_conditions , invoices.customer_notes , invoices.sub_total , invoices.tax , invoices.discount , invoices.charges , invoices.total_cost, invoices.payment_made, invoices.written_off_amount, invoices.status , line_items.line_item_id ,line_items.item_id , line_items.item_name , line_items.item_cost , line_items.item_quantity FROM invoices INNER JOIN contacts ON invoices.customer_id = contacts.contact_id INNER JOIN line_items ON invoices.invoice_id = line_items.invoice_id  WHERE invoices.invoice_id = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

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
                lineItemObject.addProperty("item_cost", rs.getFloat("item_cost"));
                lineItemObject.addProperty("item_quantity", rs.getInt("item_quantity"));
                lineItemObject.addProperty("amount", (rs.getFloat("item_cost") * rs.getInt("item_quantity")));

                jsonArray.add(lineItemObject);

            }

            responseJson.add("line_items", jsonArray);

            rs.previous();

            responseJson.addProperty("sub_total", rs.getFloat("sub_total"));

            if(rs.getFloat("tax") != 0)
            {
                responseJson.addProperty("tax", rs.getFloat("tax"));
            }
            if(rs.getFloat("discount") != 0)
            {
                responseJson.addProperty("discount", rs.getFloat("discount"));
            }
            if(rs.getFloat("charges") != 0)
            {
                responseJson.addProperty("charges", rs.getFloat("charges"));
            }

            responseJson.addProperty("total_cost", rs.getFloat("total_cost"));

            if(rs.getFloat("payment_made") != 0)
            {
                responseJson.addProperty("payment_made", rs.getFloat("payment_made"));
            }
            if(rs.getFloat("written_off_amount") != 0)
            {
                responseJson.addProperty("written_off_amount" , rs.getFloat("written_off_amount"));
            }

            responseJson.addProperty("balance_due", (rs.getFloat("total_cost") -  rs.getFloat("payment_made") - rs.getFloat("written_off_amount")));

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

    public static void markAsDraft(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if(!Filters.ifInvoiceExists(invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            while (set.next()) {

                if (set.getString("status").equals("PAID") || set.getString("status").equals("PARTIALLY PAID") || set.getString("status").equals("SENT")) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Open invoices cannot be changed to Draft");
                    return;
                }
                if (set.getString("status").equals("DRAFT")) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Invoice already in Draft state");
                    return;
                }
                if (set.getString("status").equals("VOID")) {
                    statement = connection.prepareStatement("UPDATE invoices SET status = 'DRAFT' where invoice_id = ? ;");
                    statement.setLong(1, invoice_id);
                    statement.executeUpdate();

                    CommonMethods.responseSender(response, "Invoice marked as Draft");
                    return;
                }

            }
        }
        catch(SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Cannot mark invoice as Draft");
        }
    }

    public static int deleteSentInvoice(long invoice_id) throws IOException
    {

        Connection connection = CommonMethods.createConnection();


        float sales = 0;

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT total_cost from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                sales = set.getFloat("total_cost");
            }

            InvoiceMethods.revertItemsSent(invoice_id, sales);

            return InvoiceMethods.deleteInvoice(invoice_id);


        }
        catch (SQLException e)
        {
            return 0;
        }

    }

    public static void revertItemsSent(long invoice_id, float sales) throws IOException
    {

        Connection connection = CommonMethods.createConnection();
        float cost_of_goods_sold = 0;

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT item_id, item_quantity, stock_rate FROM line_items where invoice_id = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            HashMap<Long, Integer> itemQuantityInInvoice = new HashMap<Long, Integer>();
            HashMap<Long, Float> itemIdStockRate = new HashMap<Long, Float>();

            while (set.next())
            {
                int quantity = set.getInt("item_quantity");

                if(itemQuantityInInvoice.containsKey(set.getLong("item_id")))
                {
                    quantity += itemQuantityInInvoice.get(set.getLong("item_id"));
                    itemQuantityInInvoice.remove(set.getLong("item_id"));
                }

                itemQuantityInInvoice.put(set.getLong("item_id"), quantity);
                itemIdStockRate.put(set.getLong("item_id"), set.getFloat("stock_rate"));
            }

            //Increase the item quantity in items table

            StringBuilder itemQuery = new StringBuilder("UPDATE items SET item_quantity = CASE item_id ");
            StringBuilder reducedItemIds = new StringBuilder();
            boolean reducedIdKey = true;

            for(long item_id : itemQuantityInInvoice.keySet())
            {
                itemQuery.append(" WHEN " + item_id + " THEN item_quantity + " + itemQuantityInInvoice.get(item_id));
                reducedIdKey = CommonMethods.conjunction(reducedIdKey, reducedItemIds);

                reducedItemIds.append(item_id);
            }

            itemQuery.append(" ELSE item_quantity END WHERE item_id IN ( " + reducedItemIds+" );");

            statement = connection.prepareStatement(itemQuery.toString());
            statement.executeUpdate();

            //Update chart of accounts

            for(long item_id : itemIdStockRate.keySet())
            {
                cost_of_goods_sold += (itemIdStockRate.get(item_id)*itemQuantityInInvoice.get(item_id));
            }

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? WHERE account_name = 'Inventory asset';");
            statement.setFloat(1, cost_of_goods_sold);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? WHERE account_name = 'Cost of Goods Sold';");
            statement.setFloat(1, cost_of_goods_sold);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? WHERE account_name = 'Sales';");
            statement.setFloat(1, sales);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, sales);
            statement.executeUpdate();

        }
        catch (SQLException e)
        {

        }
    }
    public static void markAsVoid(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if(!Filters.ifInvoiceExists(invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, total_cost from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();
            float sales = 0;

            while (set.next()) {

                if(set.getString("status").equals("PAID") || set.getString("status").equals("PARTIALLY PAID"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "You cannot mark a paid invoice as void");
                    return;
                }
                if(set.getString("status").equals("VOID"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Invoice already in void status");
                    return;
                }
                if(set.getString("status").equals("DRAFT"))
                {
                    statement = connection.prepareStatement("UPDATE invoices SET status = 'VOID' where invoice_id = ? ;");
                    statement.setLong(1, invoice_id);
                    statement.executeUpdate();

                    CommonMethods.responseSender(response, "Invoice marked as Void");
                    return;
                }

                sales = set.getFloat("total_cost");
            }

            InvoiceMethods.revertItemsSent(invoice_id, sales);

            //Update invoice status as 'Void'

            statement = connection.prepareStatement("UPDATE invoices SET status = 'VOID' where invoice_id = ? ;");
            statement.setLong(1, invoice_id);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "Invoice marked as Void");


        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Could not mark invoice as void");
        }
    }

    public static void markAsSent(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if(!Filters.ifInvoiceExists(invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }


        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT status, total_cost from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();
            float sales = 0;
            float cost_of_goods_sold = 0;

            while (set.next())
            {
                if(!set.getString("status").equals("DRAFT"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Only invoice in draft state can be marked as Sent");
                    return;
                }
                sales = set.getFloat("total_cost");
            }

            //Getting quantity of each line item

            statement = connection.prepareStatement("SELECT item_id, item_quantity FROM line_items where invoice_id = ?;");
            statement.setLong(1, invoice_id);
            set = statement.executeQuery();

            HashMap<Long, Integer> itemQuantityInInvoice = new HashMap<Long, Integer>();

            while (set.next())
            {
                int quantity = set.getInt("item_quantity");

                if(itemQuantityInInvoice.containsKey(set.getLong("item_id")))
                {
                    quantity += itemQuantityInInvoice.get(set.getLong("item_id"));
                    itemQuantityInInvoice.remove(set.getLong("item_id"));
                }

                itemQuantityInInvoice.put(set.getLong("item_id"), quantity);
            }

            //Reduce the item quantity in items table

            StringBuilder itemQuery = new StringBuilder("UPDATE items SET item_quantity = CASE item_id ");
            StringBuilder reducedItemIds = new StringBuilder();
            boolean reducedIdKey = true;

            for(long item_id : itemQuantityInInvoice.keySet())
            {
                itemQuery.append(" WHEN " + item_id + " THEN item_quantity - " + itemQuantityInInvoice.get(item_id));
                reducedIdKey = CommonMethods.conjunction(reducedIdKey, reducedItemIds);

                reducedItemIds.append(item_id);
            }

            itemQuery.append(" ELSE item_quantity END WHERE item_id IN ( " + reducedItemIds+" );");

            statement = connection.prepareStatement(itemQuery.toString());
            statement.executeUpdate();

            //Getting current stock rate of each line item

            statement = connection.prepareStatement("SELECT item_id, stock_rate FROM items WHERE item_id IN ( " + reducedItemIds + " );");
            set = statement.executeQuery();

            HashMap<Long, Float> itemIdStockRate = new HashMap<Long, Float>();

            while(set.next())
            {
                itemIdStockRate.put(set.getLong("item_id"), set.getFloat("stock_rate"));
            }

            //Update current stock rate to each line item

            itemQuery = new StringBuilder("UPDATE line_items SET stock_rate = CASE item_id ");

            for(long item_id : itemIdStockRate.keySet())
            {
                itemQuery.append(" WHEN " + item_id + " THEN " + itemIdStockRate.get(item_id));

                cost_of_goods_sold += (itemIdStockRate.get(item_id)*itemQuantityInInvoice.get(item_id));
            }

            itemQuery.append(" ELSE stock_rate END WHERE item_id IN ( " + reducedItemIds+" );");

            statement = connection.prepareStatement(itemQuery.toString());
            statement.executeUpdate();

            //Update chart of accounts

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Inventory asset';");
            statement.setFloat(1, cost_of_goods_sold);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Cost of Goods Sold';");
            statement.setFloat(1, cost_of_goods_sold);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Sales';");
            statement.setFloat(1, sales);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, sales);
            statement.executeUpdate();

            //Update invoice status as 'Sent'

            statement = connection.prepareStatement("UPDATE invoices SET status = 'SENT' where invoice_id = ? ;");
            statement.setLong(1, invoice_id);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "Invoice marked as Sent");

        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice could not be marked as sent");
        }

    }

    public static void deleteInvoicePayment(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if (!Filters.ifInvoiceExists(invoice_id)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, payment_made, written_off_amount from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            float payment_made = 0;
            String newStatus = "SENT";

            while (set.next()) {

                payment_made = set.getFloat("payment_made");

                if(!(set.getString("status").equals("PAID") || set.getString("status").equals("PARTIALLY PAID")) || payment_made == 0)
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "No payments available");
                    return;
                }

                if(set.getFloat("written_off_amount") != 0)
                {
                    newStatus = "PARTIALLY PAID";
                }
            }

            //Update Invoice balance_due and status

            statement = connection.prepareStatement("UPDATE invoices SET status = ?, payment_made = 0 where invoice_id = ? ;");
            statement.setString(1, newStatus );
            statement.setLong(2, invoice_id);
            statement.executeUpdate();

            //Update chart of accounts

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, payment_made);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? WHERE account_name = 'Petty cash';");
            statement.setFloat(1, payment_made);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "Invoice payment has been deleted");

        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Could not delete invoice payment");
        }
    }

    public static void cancelInvoiceWriteOff(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if (!Filters.ifInvoiceExists(invoice_id)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, payment_made, written_off_amount from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            float written_off_amount = 0;
            String newStatus = "SENT";

            while (set.next()) {

                written_off_amount = set.getFloat("written_off_amount");

                if (!(set.getString("status").equals("PAID") || set.getString("status").equals("PARTIALLY PAID")) || written_off_amount == 0)
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "No write offs available");
                    return;
                }

                if (set.getFloat("payment_made") != 0) {
                    newStatus = "PARTIALLY PAID";
                }
            }

            //Update Invoice balance_due and status

            statement = connection.prepareStatement("UPDATE invoices SET status = ?, written_off_amount = 0 where invoice_id = ? ;");
            statement.setString(1, newStatus );
            statement.setLong(2, invoice_id);
            statement.executeUpdate();

            //Update chart of accounts

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, written_off_amount);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? WHERE account_name = 'Bad debt';");
            statement.setFloat(1, written_off_amount);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "The write off done for this invoice has been cancelled");
        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Cannot cancel write off");
        }

    }


    public static void writeOffInvoice(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if (!Filters.ifInvoiceExists(invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, total_cost, payment_made, written_off_amount from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            float balance_due = 0;

            while (set.next()) {
                if (set.getString("status").equals("VOID"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "This feature cannot be used for a void invoice");
                    return;
                }
                if (set.getString("status").equals("DRAFT"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "This feature cannot be used for a draft invoice");
                    return;
                }

                balance_due = set.getFloat("total_cost") - set.getFloat("payment_made") - set.getFloat("written_off_amount");
            }

            if(balance_due == 0)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "This invoice cannot be written off");
                return;
            }

            //Update Invoice status and balance_due

            statement = connection.prepareStatement("UPDATE invoices SET status = ?, written_off_amount = written_off_amount + ? where invoice_id = ? ;");
            statement.setString(1, "PAID" );
            statement.setFloat(2, balance_due);
            statement.setLong(3, invoice_id);
            statement.executeUpdate();

            //Update chart of accounts

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Accounts Receivable';");
            statement.setFloat(1, balance_due);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Bad debt';");
            statement.setFloat(1, balance_due);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "Invoice has been written off successfully");

        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Cannot write off invoice");
        }


    }

    public static String getInvoiceStatus(long invoice_id)
    {

        Connection connection = CommonMethods.createConnection();

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                return set.getString("status");
            }
        }
        catch (SQLException e)
        {
            return null;
        }

        return null;
    }

    public static void recordInvoicePayment(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if (!Filters.ifInvoiceExists(invoice_id)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, total_cost, payment_made, written_off_amount from invoices where invoice_id  = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            float balance_due = 0;

            while (set.next())
            {
                if (set.getString("status").equals("VOID"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Invoice in void status. Cannot record payment");
                    return;
                }
                if (set.getString("status").equals("DRAFT"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Invoice in Draft status. Cannot record payment");
                    return;
                }

                balance_due = set.getFloat("total_cost") - set.getFloat("payment_made") - set.getFloat("written_off_amount");
            }

            JSONObject bodyJson = CommonMethods.readBodyJson(request);

            if(bodyJson.getFloat("amount_received") <= 0)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Incorrect value entered as amount");
            }
            else if(balance_due == 0 || bodyJson.getFloat("amount_received") > balance_due)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Excess amount entered");
            }

            else
            {
                String newStatus;

                if(bodyJson.getFloat("amount_received") == balance_due)
                {
                    newStatus = "PAID";
                }
                else
                {
                    newStatus = "PARTIALLY PAID";
                }

                //Update Invoice status and balance_due

                statement = connection.prepareStatement("UPDATE invoices SET status = ?, payment_made = payment_made + ? where invoice_id = ? ;");
                statement.setString(1, newStatus );
                statement.setFloat(2, bodyJson.getFloat("amount_received"));
                statement.setLong(3, invoice_id);
                statement.executeUpdate();

                //Update chart of accounts

                statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Accounts Receivable';");
                statement.setFloat(1, bodyJson.getFloat("amount_received"));
                statement.executeUpdate();

                statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Petty cash';");
                statement.setFloat(1, bodyJson.getFloat("amount_received"));
                statement.executeUpdate();

                CommonMethods.responseSender(response, "Payment recorded successfully");
            }


        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Could not record payment");
        }

    }

    public static void emailInvoice(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        Connection connection = CommonMethods.createConnection();

        long invoice_id = CommonMethods.parseId(request);

        if (!Filters.ifInvoiceExists(invoice_id)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT status, contacts.contact_name as customer_name, contacts.contact_email as customer_email, invoice_date, total_cost FROM invoices INNER JOIN contacts ON contacts.contact_id = invoices.customer_id  WHERE invoices.invoice_id = ?;");
            statement.setLong(1, invoice_id);
            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                if(set.getString("status").equals("VOID"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    CommonMethods.responseSender(response, "Invoice in void status. Could not be sent");
                    return;
                }

                CommonMethods.sendEmail(invoice_id, set.getString("customer_name"),set.getString("customer_email"), set.getInt("total_cost"), set.getString("invoice_date"));

                if (set.getString("status").equals("DRAFT"))
                {
                    InvoiceMethods.markAsSent(request, response);
                }
                else
                {
                    CommonMethods.responseSender(response, "Email sent successfully");
                }
            }


        } catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice cannot be sent");
        }

    }

    public static void invoiceFunctionRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String pathInfo = request.getPathInfo();

        String[] splits = pathInfo.split("/");
        String function = splits[2];

        if(function.equals("email"))
        {
            InvoiceMethods.emailInvoice(request, response);
        }
        else if(function.equals("writeoff"))
        {
            InvoiceMethods.writeOffInvoice(request, response);
        }
        else if(function.equals("cancelwriteoff"))
        {
            InvoiceMethods.cancelInvoiceWriteOff(request, response);
        }
        else if(function.equals("recordpayment"))
        {
            InvoiceMethods.recordInvoicePayment(request, response);
        }
        else if(function.equals("deletepayment"))
        {
            InvoiceMethods.deleteInvoicePayment(request, response);
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL passed");
        }
    }

    public static void invoiceStatusRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String pathInfo = request.getPathInfo();

        String[] splits = pathInfo.split("/");
        String status = splits[3];

        if(status.equals("sent"))
        {
            InvoiceMethods.markAsSent(request, response);
        }
        else if(status.equals("draft"))
        {
            InvoiceMethods.markAsDraft(request, response);
        }
        else if(status.equals("void"))
        {
            InvoiceMethods.markAsVoid(request, response);
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL passed");
        }
    }


}
