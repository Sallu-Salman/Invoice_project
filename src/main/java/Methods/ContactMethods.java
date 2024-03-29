package Methods;

import Templates.Contact_json;

import javax.servlet.http.HttpServletResponse;
import java.sql.*;


public class ContactMethods
{
    public static Contact_json getContactDetails(long contact_id)
    {
        Contact_json contact_json = new Contact_json();

        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM contacts WHERE contact_id=?;");

            statement.setLong(1, contact_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                contact_json.contact_id = rs.getLong("contact_id");
                contact_json.contact_name = rs.getString("contact_name");
                contact_json.contact_email = rs.getString("contact_email");
                contact_json.contact_phone = rs.getString("contact_phone");
            }

            connection.close();

            return contact_json;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }


    }

    public static Contact_json[] getContactsDetails()
    {

        Connection connection =  CommonMethods.createConnection();
        int rows = 1;

        try
        {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = statement.executeQuery("SELECT * FROM contacts;");

            if (rs.last()) {
                rows = rs.getRow();
                rs.beforeFirst();
            }

            Contact_json[] contact_jsons = new Contact_json[rows];

            for(int i=0; rs.next(); i++)
            {
                contact_jsons[i] = new Contact_json();

                contact_jsons[i].contact_id = rs.getLong("contact_id");
                contact_jsons[i].contact_name = rs.getString("contact_name");
                contact_jsons[i].contact_email = rs.getString("contact_email");
                contact_jsons[i].contact_phone = rs.getString("contact_phone");
            }
            connection.close();

            return contact_jsons;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static long createNewContact(Contact_json contact_json)
    {
        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO contacts(contact_name, contact_email, contact_phone) VALUES (?,?,?);", Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, contact_json.contact_name);
            statement.setString(2, contact_json.contact_email);
            statement.setString(3, contact_json.contact_phone);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();
            long generatedId = -1;

            if(set.next())
            {
                generatedId = set.getLong(1);
            }

            connection.close();
            return generatedId;

        }
        catch (SQLException e)
        {
            return -1;
        }

    }

    public static int deleteContact(long contact_id)
    {
        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM contacts WHERE contact_id = ?");
            statement.setLong(1, contact_id);
            int affected_rows = statement.executeUpdate();

            connection.close();
            return affected_rows;
        }
        catch (SQLException e)
        {
            return 0;
        }

    }

    public static int updateContact(Contact_json contact_json)
    {
        Connection connection =  CommonMethods.createConnection();


        StringBuilder query = new StringBuilder("UPDATE contacts SET ");
        boolean key = true;

        if(contact_json.contact_name != null)
        {
            key = CommonMethods.conjunction(key, query);

            query.append(" contact_name = '"+contact_json.contact_name + "' ");
        }

        if(contact_json.contact_email != null)
        {
            key = CommonMethods.conjunction(key, query);

            query.append(" contact_email = '"+contact_json.contact_email + "' ");
        }

        if(contact_json.contact_phone != null)
        {
            CommonMethods.conjunction(key, query);

            query.append(" contact_phone = '"+contact_json.contact_phone + "' ");
        }

        query.append(" WHERE contact_id = "+contact_json.contact_id + ";");


        try
        {
            PreparedStatement statement = connection.prepareStatement(query.toString());
            int affected_rows = statement.executeUpdate();
            connection.close();

            return affected_rows;
        }
        catch(SQLException e)
        {
            return 0;
        }

    }

}
