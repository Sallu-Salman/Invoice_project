package Methods;

import Templates.Contact_json;
import Templates.Item_json;

import java.sql.*;

public class ItemMethods
{
    public long createNewItem(Item_json item_json)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO items(item_name, item_cost, item_quantity) VALUES (?,?,?);", Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, item_json.item_name);
            statement.setInt(2, item_json.item_cost);
            statement.setInt(3, item_json.item_quantity);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();

            if(set.next())
            {
                return set.getLong(1);
            }

            return -1;

        }
        catch (SQLException e)
        {
            return -1;
        }
    }

    public Item_json[] getItemsDetails()
    {
        CommonMethods commonMethods = new CommonMethods();

        Connection connection =  commonMethods.createConnection();
        int rows = 1;

        try
        {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = statement.executeQuery("SELECT * FROM items;");

            if (rs.last()) {
                rows = rs.getRow();
                rs.beforeFirst();
            }

            Item_json[] item_jsons = new Item_json[rows];

            for(int i=0; rs.next(); i++)
            {
                item_jsons[i] = new Item_json();

                item_jsons[i].item_id = rs.getLong("item_id");
                item_jsons[i].item_name = rs.getString("item_name");
                item_jsons[i].item_cost = rs.getInt("item_cost");
                item_jsons[i].item_quantity = rs.getInt("item_quantity");
            }

            return item_jsons;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Item_json getItemDetails(long item_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Item_json item_json = new Item_json();

        Connection connection =  commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM items WHERE item_id=?;");

            statement.setLong(1, item_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                item_json.item_id = rs.getLong("item_id");
                item_json.item_name = rs.getString("item_name");
                item_json.item_cost = rs.getInt("item_cost");
                item_json.item_quantity = rs.getInt("item_quantity");
            }

            return item_json;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int deleteItem(long item_id)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE item_id = ?");
            statement.setLong(1, item_id);

            return statement.executeUpdate();
        }
        catch (SQLException e)
        {
            return 0;
        }

    }

    public int updateItem(Item_json item_json)
    {
        CommonMethods commonMethods = new CommonMethods();
        Connection connection =  commonMethods.createConnection();


        StringBuilder query = new StringBuilder("UPDATE items SET ");
        boolean key = true;

        if(item_json.item_name != null)
        {
            key = commonMethods.conjunction(key, query);

            query.append(" item_name = '"+item_json.item_name + "' ");
        }

        if(item_json.item_cost >= 0)
        {
            key = commonMethods.conjunction(key, query);

            query.append(" item_cost = '"+item_json.item_cost + "' ");
        }

        if(item_json.item_quantity >= 0)
        {
            commonMethods.conjunction(key, query);

            query.append(" item_quantity = '" + item_json.item_quantity + "' ");
        }

        query.append(" WHERE item_id = "+item_json.item_id + " ;");

        try
        {
            PreparedStatement statement = connection.prepareStatement(query.toString());

            return statement.executeUpdate();
        }
        catch(SQLException e)
        {
            return 0;
        }
    }


}
