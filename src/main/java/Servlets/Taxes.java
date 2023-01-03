package Servlets;

import Methods.CommonMethods;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/api/taxes/*")
public class Taxes extends HttpServlet
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

        try
        {
            Connection connection = CommonMethods.createConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO taxes(tax_name, tax_percentage, is_group) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, inputJson.getString("tax_name"));
            statement.setInt(2, inputJson.getInt("tax_percentage"));
            statement.setInt(3, 0);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();
            long generatedId = -1;

            if(set.next())
            {
                generatedId = set.getLong(1);
            }

            inputJson.put("tax_id", generatedId);

            CommonMethods.responseObjectSender(response, "Tax created", inputJson.toString());

        }
        catch (SQLException e) {

            CommonMethods.responseSender(response, "Something went wrong. Tax not created");
        }
    }
}
