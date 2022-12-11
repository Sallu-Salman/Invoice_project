package Methods;

import java.util.regex.*;
import Templates.Contact_json;
import Templates.Item_json;
import org.json.JSONObject;

public class Filters {
    public boolean checkPhoneNumber(String phoneNumber) {
        String regex = "((\\+*)((0[ -]*)*|((91 )*))((\\d{12})+|(\\d{10})+))|\\d{5}([- ]*)\\d{6}";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(phoneNumber);

        return m.matches();
    }

    public boolean checkEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(email);

        return m.matches();
    }

    public boolean checkContact(Contact_json contact_json) {
        Filters filters = new Filters();

        if (contact_json.contact_name == null && contact_json.contact_email == null && contact_json.contact_phone == null) {
            return false;
        }

        if (contact_json.contact_phone != null && !filters.checkPhoneNumber(contact_json.contact_phone)) {
            return false;
        }

        if (contact_json.contact_email != null && !filters.checkEmail(contact_json.contact_email)) {
            return false;
        }

        return true;
    }

    public boolean checkItem(Item_json item_json) {
        if (item_json.item_cost < 0 || item_json.item_quantity < 0) {
            return false;
        }

        return true;
    }

    public Item_json checkAndLoadItem(JSONObject jsonObject)
    {
        Item_json item_json = new Item_json();

        if(jsonObject.has("item_quantity"))
        {
            item_json.item_quantity = jsonObject.getInt("item_quantity");
        }
        else
        {
            item_json.item_quantity = -1;
        }

        if(jsonObject.has("item_cost"))
        {
            item_json.item_cost = jsonObject.getInt("item_cost");
        }
        else
        {
            item_json.item_cost = -1;
        }

        if(jsonObject.has("item_name"))
        {
            item_json.item_name = jsonObject.getString("item_name");
        }

        return item_json;
    }
}
