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

@WebServlet("/api/invoices/*")
public class Invoice extends HttpServlet
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.emptyPath(request.getPathInfo()) && CommonMethods.invoiceFunctionPath(request.getPathInfo()))
        {
            //redirect to invoiceFunction
            InvoiceMethods.invoiceFunctionRedirect(request, response);
            return;
        }

        if(!CommonMethods.emptyPath(request.getPathInfo()) && CommonMethods.invoiceStatusPath(request.getPathInfo()))
        {
            //redirect to status
            InvoiceMethods.invoiceStatusRedirect(request, response);
            return;
        }

        if(!CommonMethods.emptyPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();

        Invoice_json invoice_json = gson.fromJson(reader, Invoice_json.class);
        Item_json[] line_item_json = gson.fromJson(invoice_json.line_items, Item_json[].class);

        if(line_item_json == null || (line_item_json = InvoiceMethods.fetchLineItemsDetails(line_item_json)) == null)
        {
            response.getWriter().println("No valid items !\nInvoice cannot be raised");
            return;
        }

        invoice_json.sub_total = InvoiceMethods.findSubTotal(line_item_json);

        invoice_json.invoice_id = InvoiceMethods.raiseInvoice(invoice_json);

        if(invoice_json.invoice_id == -1 )
        {
            response.getWriter().println("Something went wrong ! \nInvoice cannot be raised");
        }
        else if (!InvoiceMethods.storeLineItems(line_item_json, invoice_json.invoice_id))
        {
            InvoiceMethods.deleteInvoice(invoice_json.invoice_id);
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
        if(!(CommonMethods.emptyPath(request.getPathInfo()) || CommonMethods.paramPath(request.getPathInfo())))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }



        String responseJson;

        if (CommonMethods.emptyPath(request.getPathInfo()))
        {
            responseJson = InvoiceMethods.getInvoicesDetails();
        }
        else
        {
            long invoice_id = CommonMethods.parseId(request);

            responseJson = InvoiceMethods.getInvoiceDetails(invoice_id);

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
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }


        JSONObject inputJson = InvoiceMethods.readBodyJson(request);

        if(inputJson == null)
        {
            response.getWriter().println("Invalid data provided");
            return;
        }

        inputJson.put("invoice_id", CommonMethods.parseId(request));

        if(!Filters.ifInvoiceExists(inputJson.getLong("invoice_id")))
        {
            response.getWriter().println("Invoice does not exists");
            return;
        }

        String invoice_status = InvoiceMethods.getInvoiceStatus(inputJson.getLong("invoice_id"));

        if(invoice_status.equals("PAID") || invoice_status.equals("PARTIALLY PAID"))
        {
            response.getWriter().println("Payment already initiated. Cannot edit invoice now");
        }
        else if(invoice_status.equals("VOID"))
        {
            response.getWriter().println("Invoice at Void status cannot be editted");
        }
        else if(invoice_status.equals("SENT") && (InvoiceMethods.updateInvoice(inputJson, "SENT") != 0))
        {
            response.getWriter().println("Invoice updates successfully");
        }
        else if( InvoiceMethods.updateInvoice(inputJson, "DRAFT") != 0)
        {
            response.getWriter().println("Invoice updated successfully");
        }
        else
        {
            response.getWriter().println("Something went wrong !\nInvoice was not updated");
        }

    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.getWriter().println("Invalid URL passed");
            return;
        }



        long invoice_id = CommonMethods.parseId(request);

        if(!Filters.ifInvoiceExists(invoice_id))
        {
            response.getWriter().println("Invoice does not exists");
            return;
        }

        String invoice_status = InvoiceMethods.getInvoiceStatus(invoice_id);

        if(invoice_status.equals("PAID") || invoice_status.equals("PARTIALLY PAID"))
        {
            response.getWriter().println("Invoice contains payments");
        }
        else if(invoice_status.equals("SENT") && InvoiceMethods.deleteSentInvoice(invoice_id) != 0)
        {
            response.getWriter().println("Invoice deleted successfully");
        }

        else if(InvoiceMethods.deleteInvoice(invoice_id) != 0)
        {
            response.getWriter().println("Invoice deleted successfully !");
        }
        else
        {
            response.getWriter().println("Something went wrong !\nInvoice was not deleted");
        }
    }
}
