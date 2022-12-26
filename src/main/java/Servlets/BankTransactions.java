package Servlets;

import Methods.CommonMethods;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/api/banktransactions/*")
public class BankTransactions extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.emptyPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }

        JSONObject inputJson = CommonMethods.readBodyJson(request);

        try
        {
            Connection connection = CommonMethods.createConnection();
            PreparedStatement statement;

            if(inputJson == null)
            {
                response.getWriter().println("Invalid data provided");
                return;
            }

            String pettyAction;
            String equityAction;

            if(inputJson.getString("transaction_type").equals("owner_drawings"))
            {
                pettyAction = "credit";
                equityAction = "debit";
            }
            else if(inputJson.getString("transaction_type").equals("owner_contributions"))
            {
                pettyAction = "debit";
                equityAction = "credit";
            }
            else
            {
                response.getWriter().println("Invalid data provided");
                return;
            }

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET "+pettyAction+" = "+pettyAction+" + ? WHERE account_name = 'Petty cash';");
            statement.setFloat(1, inputJson.getFloat("amount"));
            statement.executeUpdate();

            statement = connection.prepareStatement("UPDATE chart_of_accounts SET "+equityAction+" = "+equityAction+" + ? WHERE account_name = 'Owners Equity';");
            statement.setFloat(1, inputJson.getFloat("amount"));
            statement.executeUpdate();

            response.getWriter().println("The bank transaction has been recorded");
        }
        catch (SQLException e)
        {
            response.getWriter().println("Something went wrong");
        }
        catch (JSONException e)
        {
            response.getWriter().println("Something went wrong");
        }

    }
}
