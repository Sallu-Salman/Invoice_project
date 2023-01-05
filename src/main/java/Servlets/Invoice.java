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
import java.util.HashMap;

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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();

        Invoice_json invoice_json = gson.fromJson(reader, Invoice_json.class);
        Item_json[] line_item_json = gson.fromJson(invoice_json.line_items, Item_json[].class);

        if(line_item_json == null || (line_item_json = InvoiceMethods.fetchLineItemsDetails(line_item_json)) == null)
        {
            CommonMethods.responseSender(response, "No valid items. Invoice cannot be raised");
            return;
        }

        invoice_json.sub_total = InvoiceMethods.findSubTotal(line_item_json);
        invoice_json.tax = InvoiceMethods.findTax(line_item_json);

        HashMap<Long, Float> tax_amount_split = InvoiceMethods.splitInvoiceTax(line_item_json);

        invoice_json.invoice_id = InvoiceMethods.raiseInvoice(invoice_json);

        if(invoice_json.invoice_id == -1 || !InvoiceMethods.insertInvoiceTaxDetails(tax_amount_split, invoice_json.invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice cannot be raised");
        }
        else if (!InvoiceMethods.storeLineItems(line_item_json, invoice_json.invoice_id))
        {
            InvoiceMethods.deleteInvoice(invoice_json.invoice_id);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice cannot be raised");
        }
        else
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("invoice_id", invoice_json.invoice_id);
            jsonObject.put("status", "DRAFT");

            CommonMethods.responseObjectSender(response, "Invoice has been created", jsonObject.toString());
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!(CommonMethods.emptyPath(request.getPathInfo()) || CommonMethods.paramPath(request.getPathInfo())))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }



        String responseJson;

        if (CommonMethods.emptyPath(request.getPathInfo()))
        {
            responseJson = InvoiceMethods.getInvoicesDetails();
            CommonMethods.responseArraySender(response, "success", responseJson);
        }
        else
        {
            long invoice_id = CommonMethods.parseId(request);

            responseJson = InvoiceMethods.getInvoiceDetails(invoice_id);

            if(responseJson == null)
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                CommonMethods.responseSender(response, "Invalid data Passed");
                return;
            }

            CommonMethods.responseObjectSender(response, "success", responseJson);

        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }


        JSONObject inputJson = CommonMethods.readBodyJson(request);

        if(inputJson == null)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid data Passed");
            return;
        }

        inputJson.put("invoice_id", CommonMethods.parseId(request));

        if(!Filters.ifInvoiceExists(inputJson.getLong("invoice_id")))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        String invoice_status = InvoiceMethods.getInvoiceStatus(inputJson.getLong("invoice_id"));

        if(invoice_status.equals("PAID") || invoice_status.equals("PARTIALLY PAID"))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Payment already initiated. Cannot edit invoice now");
        }
        else if(invoice_status.equals("VOID"))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice in Void status cannot be edited");
        }
        else if(invoice_status.equals("SENT") && (InvoiceMethods.updateInvoice(inputJson, "SENT") != 0))
        {
            CommonMethods.responseSender(response, "Invoice has been updated");
        }
        else if( InvoiceMethods.updateInvoice(inputJson, "DRAFT") != 0)
        {
            CommonMethods.responseSender(response, "Invoice has been updated");
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice was not updated");
        }

    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(!CommonMethods.paramPath(request.getPathInfo()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invalid URL Passed");
            return;
        }



        long invoice_id = CommonMethods.parseId(request);

        if(!Filters.ifInvoiceExists(invoice_id))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice does not exists");
            return;
        }

        String invoice_status = InvoiceMethods.getInvoiceStatus(invoice_id);

        if(invoice_status.equals("PAID") || invoice_status.equals("PARTIALLY PAID"))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CommonMethods.responseSender(response, "Invoice containing payments cannot be deleted");

        }
        else if(invoice_status.equals("SENT") && InvoiceMethods.deleteSentInvoice(invoice_id) != 0)
        {
            CommonMethods.responseSender(response, "Invoice has been deleted");
        }

        else if(InvoiceMethods.deleteInvoice(invoice_id) != 0)
        {
            CommonMethods.responseSender(response, "Invoice has been deleted");
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            CommonMethods.responseSender(response, "Something went wrong. Invoice was not deleted");
        }
    }
}
