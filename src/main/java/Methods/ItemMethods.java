package Methods;

import Templates.Contact_json;
import Templates.Item_json;

import java.sql.*;

public class ItemMethods
{
    public static long createNewItem(Item_json item_json)
    {

        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO items(item_name, item_cost, item_quantity, stock_rate, item_tax) VALUES (?,?,?,?,?);", Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, item_json.item_name);
            statement.setFloat(2, item_json.item_cost);
            statement.setInt(3, item_json.item_quantity);
            statement.setFloat(4, item_json.stock_rate);
            statement.setLong(5, item_json.item_tax);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Inventory asset';");
            statement.setFloat(1, (item_json.stock_rate* item_json.item_quantity));
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Petty cash';");
            statement.setFloat(1, (item_json.stock_rate* item_json.item_quantity));
            statement.executeUpdate();

            if(set.next())
            {
                return set.getLong(1);
            }

            connection.close();

            return -1;

        }
        catch (SQLException e)
        {
            return -1;
        }
    }

    public static Item_json[] getItemsDetails()
    {


        Connection connection =  CommonMethods.createConnection();
        int rows = 1;

        try
        {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = statement.executeQuery("SELECT * FROM items INNER JOIN taxes on items.item_tax = taxes.tax_id;");

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
                item_jsons[i].item_cost = rs.getFloat("item_cost");
                item_jsons[i].item_quantity = rs.getInt("item_quantity");
                item_jsons[i].stock_rate = rs.getFloat("stock_rate");
                item_jsons[i].item_tax = rs.getLong("item_tax");
                item_jsons[i].item_tax_name = rs.getString("tax_name");
                item_jsons[i].tax_percentage = rs.getInt("tax_percentage");
            }

            connection.close();
            return item_jsons;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Item_json getItemDetails(long item_id)
    {

        Item_json item_json = new Item_json();

        Connection connection =  CommonMethods.createConnection();

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM items INNER JOIN taxes ON items.item_tax = taxes.tax_id WHERE item_id=?;");

            statement.setLong(1, item_id);

            ResultSet rs = statement.executeQuery();

            while(rs.next())
            {
                item_json.item_id = rs.getLong("item_id");
                item_json.item_name = rs.getString("item_name");
                item_json.item_cost = rs.getFloat("item_cost");
                item_json.item_quantity = rs.getInt("item_quantity");
                item_json.stock_rate = rs.getFloat("stock_rate");
                item_json.item_tax = rs.getLong("item_tax");
                item_json.item_tax_name = rs.getString("tax_name");
                item_json.tax_percentage = rs.getInt("tax_percentage");
            }

            connection.close();

            return item_json;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static int deleteItem(long item_id)
    {

        Connection connection =  CommonMethods.createConnection();

        int old_quantity = 0;
        float old_stock_rate = 0;
        float old_total_value = 0;

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT item_quantity, stock_rate FROM items WHERE item_id = ?");
            statement.setLong(1, item_id);

            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                old_quantity = set.getInt("item_quantity");
                old_stock_rate = set.getFloat("stock_rate");

                old_total_value = old_quantity * old_stock_rate;
            }

            statement = connection.prepareStatement("DELETE FROM items WHERE item_id = ?");
            statement.setLong(1, item_id);

            int affectedRows = statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? WHERE account_name = 'Petty cash';");
            statement.setFloat(1, old_total_value);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? WHERE account_name = 'Inventory asset';");
            statement.setFloat(1, old_total_value);
            statement.executeUpdate();

            connection.close();

            return affectedRows;
        }
        catch (SQLException e)
        {
            return 0;
        }

    }

    public static int updateItem(Item_json item_json)
    {

        Connection connection =  CommonMethods.createConnection();


        StringBuilder query = new StringBuilder("UPDATE items SET ");
        boolean key = true;

        int old_quantity = 0;
        float old_stock_rate = 0;
        float old_total_value = 0;

        float new_total_value = 0;

        boolean isQuantityChanged = false;
        boolean isStockRateChanged = false;

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT item_quantity, stock_rate FROM items WHERE item_id = ?");
            statement.setLong(1, item_json.item_id);

            ResultSet set = statement.executeQuery();

            while (set.next())
            {
                old_quantity = set.getInt("item_quantity");
                old_stock_rate = set.getFloat("stock_rate");

                old_total_value = old_quantity * old_stock_rate;
            }

            if(item_json.item_name != null)
            {
                key = CommonMethods.conjunction(key, query);

                query.append(" item_name = '"+item_json.item_name + "' ");
            }

            if(item_json.item_cost >= 0)
            {
                key = CommonMethods.conjunction(key, query);

                query.append(" item_cost = "+item_json.item_cost );
            }

            if(item_json.item_quantity >= 0)
            {
                key = CommonMethods.conjunction(key, query);

                query.append(" item_quantity = " + item_json.item_quantity);

                new_total_value = (old_total_value/old_quantity) * item_json.item_quantity;

                isQuantityChanged = true;
            }

            if(item_json.stock_rate >=  0)
            {
                CommonMethods.conjunction(key, query);

                query.append(" stock_rate = " + item_json.stock_rate);

                new_total_value = (old_total_value/old_stock_rate) * item_json.stock_rate;

                isStockRateChanged = true;
            }

            if(item_json.item_tax >= 0)
            {
                CommonMethods.conjunction(key, query);

                query.append(" item_tax = " + item_json.item_tax);
            }

            query.append(" WHERE item_id = "+item_json.item_id + " ;");


            statement = connection.prepareStatement(query.toString());

            int affected_rows = statement.executeUpdate();

            if(isQuantityChanged && isStockRateChanged)
            {
                new_total_value = (item_json.stock_rate * item_json.item_quantity);
            }

            if(isQuantityChanged || isStockRateChanged)
            {
                statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit - ? + ? WHERE account_name = 'Petty cash';");
                statement.setFloat(1, old_total_value);
                statement.setFloat(2, new_total_value);

                statement.executeUpdate();

                statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit - ? + ? WHERE account_name = 'Inventory asset';");
                statement.setFloat(1, old_total_value);
                statement.setFloat(2, new_total_value);

                statement.executeUpdate();
            }

            connection.close();

            return affected_rows;
        }
        catch(SQLException e)
        {
            return 0;
        }
    }


}
