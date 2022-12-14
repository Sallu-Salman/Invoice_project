package Templates;

import com.google.gson.JsonArray;
import org.json.JSONArray;

public class Invoice_json
{
    public long invoice_id;
    public long customer_id;
    public long salesperson_id;

    public String customer_name;
    public String invoice_date;
    public String salesperson_name;

    public String subject;
    public String terms_and_conditions;
    public String customer_notes;

    public int sub_total;
    public int tax;
    public int discount;
    public int charges;
    public int total_cost;

    public String status;
    public JsonArray line_items;

}
