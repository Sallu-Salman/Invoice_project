package Servlets;

import Methods.CommonMethods;
import Methods.ContactMethods;
import Methods.Filters;
import Methods.InvoiceMethods;
import Templates.Contact_json;
import Templates.Invoice_json;
import Templates.Item_json;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/invoices/*")
public class Invoice extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        InvoiceMethods invoiceMethods = new InvoiceMethods();

        Invoice_json invoice_json = gson.fromJson(reader, Invoice_json.class);
        Item_json[] line_item_json = gson.fromJson(invoice_json.line_items, Item_json[].class);

        if(line_item_json == null || (line_item_json = invoiceMethods.fetchLineItemsDetails(line_item_json)) == null)
        {
            response.getWriter().println("No valid items !\nInvoice cannot be raised");
            return;
        }

        invoice_json.sub_total = invoiceMethods.findSubTotal(line_item_json);

        invoice_json.invoice_id = invoiceMethods.raiseInvoice(invoice_json);

        if(invoice_json.invoice_id == -1 )
        {
            response.getWriter().println("Something went wrong ! \nInvoice cannot be raised");
        }
        else if (!invoiceMethods.storeLineItems(line_item_json, invoice_json.invoice_id))
        {
            invoiceMethods.deleteInvoice(invoice_json.invoice_id);
            response.getWriter().println("Something went wrong ! \nInvoice cannot be raised");
        }
        else
        {
            response.getWriter().println("The invoice has been created");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("invoice_id", invoice_json.invoice_id);
            jsonObject.put("status", "DRAFT");

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonObject.toString());
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InvoiceMethods invoiceMethods = new InvoiceMethods();
        CommonMethods commonMethods = new CommonMethods();

        long invoice_id = commonMethods.parseId(request);
        String responseJson;

        if (invoice_id == -1)
        {
            responseJson = invoiceMethods.getInvoicesDetails();
        }
        else
        {
            responseJson = invoiceMethods.getInvoiceDetails(invoice_id);

            if(responseJson == null)
            {
                response.getWriter().println("Invalid data passed !");
                return;
            }

        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InvoiceMethods invoiceMethods = new InvoiceMethods();
        CommonMethods commonMethods = new CommonMethods();

        JSONObject inputJson = invoiceMethods.readBodyJson(request);

        if(inputJson == null)
        {
            response.getWriter().println("Invalid data provided");
            return;
        }

        inputJson.put("invoice_id", commonMethods.parseId(request));

        if(inputJson.getLong("invoice_id") == -1 || invoiceMethods.updateInvoice(inputJson) == 0)
        {
            response.getWriter().println("Something went wrong !\nInvoice was not updated");
        }
        else
        {
            response.getWriter().println("Invoice updated successfully");
        }

    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InvoiceMethods invoiceMethods = new InvoiceMethods();
        CommonMethods commonMethods = new CommonMethods();

        long invoice_id = commonMethods.parseId(request);

        if(invoiceMethods.deleteInvoice(invoice_id) == 0)
        {
            response.getWriter().println("Something went wrong !\nInvoice was not deleted");
        }
        else
        {
            response.getWriter().println("Invoice deleted successfully !");
        }
    }
}
