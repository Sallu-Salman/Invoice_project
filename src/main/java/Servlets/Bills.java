package Servlets;

import Methods.CommonMethods;
import Methods.Filters;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/bills/*")
public class Bills extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.emptyPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }

        JSONObject inputJson = CommonMethods.readBodyJson(request);

        try {
            Connection connection = CommonMethods.createConnection();
            PreparedStatement statement;
            float stock_rate = 0;

            StringBuilder query = new StringBuilder("UPDATE items SET ");

            if (inputJson == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Invalid data Passed");
                return;
            }
            if(!Filters.ifItemExists(inputJson.getLong("item_id")))
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Item does not exists");
                return;
            }

            if(inputJson.has("stock_rate"))
            {
                //Weighted average cost : stock_rate = ((item_quantity*stock_rate)+(new_quantity*new_stock_rate))/(item_quantity+new_quantity)

                query.append(" stock_rate = ((item_quantity*stock_rate)+("+inputJson.getInt("item_quantity")+"*"+inputJson.getFloat("stock_rate")+"))/(item_quantity+"+inputJson.getInt("item_quantity")+") , ");
                stock_rate = inputJson.getFloat("stock_rate");
            }
            else
            {
                statement = connection.prepareStatement("SELECT stock_rate FROM items WHERE item_id = ?; ");
                statement.setLong(1, inputJson.getLong("item_id"));
                ResultSet set = statement.executeQuery();

                while (set.next())
                {
                    stock_rate = set.getFloat("stock_rate");
                }
            }

            query.append(" item_quantity = item_quantity + "+inputJson.getInt("item_quantity")+" WHERE item_id = "+inputJson.getLong("item_id")+";");

            statement = connection.prepareStatement(query.toString());
            statement.executeUpdate();

            // Update Inventory asset

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET credit = credit + ? WHERE account_name = 'Petty Cash';");
            statement.setFloat(1, inputJson.getInt("item_quantity")*stock_rate);
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET debit = debit + ? WHERE account_name = 'Inventory asset';");
            statement.setFloat(1, inputJson.getInt("item_quantity")*stock_rate);
            statement.executeUpdate();

            CommonMethods.responseSender(response, "Bill created successfully");

        }
        catch (SQLException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong");
        }
        catch (JSONException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong");
        }
    }
}
