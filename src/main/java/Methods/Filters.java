package Methods;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.*;
import Templates.Contact_json;
import Templates.Item_json;
import org.json.JSONArray;
import org.json.JSONObject;

public class Filters {
    public boolean checkPhoneNumber(String phoneNumber) {
        String regex = "((\\+*)((0[ -]*)*|((91 )*))((\\d{12})+|(\\d{10})+))|\\d{5}([- ]*)\\d{6}";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(phoneNumber);

        return m.matches();
    }

    public boolean checkEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(email);

        return m.matches();
    }

    public boolean checkContact(Contact_json contact_json) {
        Filters filters = new Filters();

        if (contact_json.contact_name == null && contact_json.contact_email == null && contact_json.contact_phone == null) {
            return false;
        }

        if (contact_json.contact_phone != null && !filters.checkPhoneNumber(contact_json.contact_phone)) {
            return false;
        }

        if (contact_json.contact_email != null && !filters.checkEmail(contact_json.contact_email)) {
            return false;
        }

        return true;
    }

    public boolean ifSalespersonExists(long salesperson_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as total from salespersons where salesperson_id = ?;");

            statement.setLong(1, salesperson_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                if(rs.getInt("total") == 0)
                {
                    return false;
                }
            }

            connection.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    public boolean ifItemsExists(JSONArray newLineItems)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();
        HashSet<Long> item_ids = new HashSet<Long>();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) as total FROM items WHERE item_id IN (");
        boolean key = true;

        for(int i=0; i<newLineItems.length(); i++)
        {
            JSONObject jsonObject = newLineItems.getJSONObject(i);

            if(jsonObject.length() == 0)
            {
                return false;
            }

            if(jsonObject.has("line_item_id"))
            {
                continue;
            }

            key = commonMethods.conjunction(key, query);
            query.append(jsonObject.getLong("item_id"));
            item_ids.add(jsonObject.getLong("item_id"));
        }

        query.append(");");

        if(item_ids.size() == 0)
        {
            return true;
        }

        try
        {
            PreparedStatement statement = connection.prepareStatement(query.toString());

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                if(rs.getInt("total") != item_ids.size())
                {
                    return false;
                }
            }

            connection.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }


    }

    public boolean ifInvoiceExists(long invoice_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as total from invoices where invoice_id = ?;");

            statement.setLong(1, invoice_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                if(rs.getInt("total") == 0)
                {
                    return false;
                }
            }

            connection.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    public boolean ifCustomerExists(long contact_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as total from contacts where contact_id = ?;");

            statement.setLong(1, contact_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                if(rs.getInt("total") == 0)
                {
                    return false;
                }
            }

            connection.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    public boolean ifItemExists(long item_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection = commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as total from items where item_id = ?;");

            statement.setLong(1, item_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                if(rs.getInt("total") == 0)
                {
                    return false;
                }
            }

            connection.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
    }


    public Item_json checkAndLoadItem(JSONObject jsonObject)
    {
        Item_json item_json = new Item_json();

        if(jsonObject.has("item_quantity"))
        {
            item_json.item_quantity = jsonObject.getInt("item_quantity");
        }
        else
        {
            item_json.item_quantity = -1;
        }

        if(jsonObject.has("item_cost"))
        {
            item_json.item_cost = jsonObject.getInt("item_cost");
        }
        else
        {
            item_json.item_cost = -1;
        }

        if(jsonObject.has("stock_rate"))
        {
            item_json.stock_rate = jsonObject.getInt("stock_rate");
        }
        else
        {
            item_json.stock_rate = -1;
        }

        if(jsonObject.has("item_name"))
        {
            item_json.item_name = jsonObject.getString("item_name");
        }

        return item_json;
    }

    public boolean checkSubject (String subject)
    {
        if(subject.length() > 200)
        {
            return false;
        }

        return true;
    }

    public boolean checkTermsAndConditions (String termsAndConditions)
    {
        if(termsAndConditions.length() > 200)
        {
            return false;
        }

        return true;
    }
    public boolean checkCustomerNotes (String customerNotes)
    {
        if(customerNotes.length() > 200)
        {
            return false;
        }

        return true;
    }



}
