package Servlets;

import Methods.CommonMethods;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/api/taxgroups/*")
public class TaxGroup extends HttpServlet
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

            StringBuilder builder = new StringBuilder();

            JSONArray tax_ids = inputJson.optJSONArray("taxes_associated");
            boolean key = true;

            for(int i=0; i<tax_ids.length(); i++)
            {
                key = CommonMethods.conjunction(key, builder);
                builder.append(tax_ids.optInt(i));
            }

            PreparedStatement statement = connection.prepareStatement("SELECT SUM(tax_percentage) as total FROM taxes WHERE  tax_id IN ( "+builder+" );");

            ResultSet rs = statement.executeQuery();

            int tax_percentage = 0;

            while(rs.next())
            {
                tax_percentage = rs.getInt("total");
            }

            statement = connection.prepareStatement("INSERT INTO taxes(tax_name, tax_percentage, is_group) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, inputJson.getString("tax_group_name"));
            statement.setInt(2, tax_percentage);
            statement.setInt(3, 1);

            statement.executeUpdate();

            ResultSet set = statement.getGeneratedKeys();
            long generatedId = -1;

            if(set.next())
            {
                generatedId = set.getLong(1);
            }

            inputJson.put("tax_percentage", tax_percentage);
            inputJson.put("tax_id", generatedId);

            builder = new StringBuilder("INSERT INTO tax_groups(tax_group_id, tax_id) VALUES ");

            key = true;
            for(int i=0; i<tax_ids.length(); i++)
            {
                key = CommonMethods.conjunction(key, builder);

                builder.append("( "+generatedId+ " , "+ tax_ids.optInt(i)+ " )");
            }

            builder.append(";");

            statement = connection.prepareStatement(builder.toString());
            statement.execute();

            CommonMethods.responseObjectSender(response, "Tax group created", inputJson.toString());

        }
        catch (Exception e)
        {
            CommonMethods.responseSender(response, "Tax group not created");
        }
    }
}
