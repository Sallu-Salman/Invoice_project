package Methods;

import java.util.regex.*;
import Templates.Contact_json;

public class Filters
{
    public boolean checkPhoneNumber(String phoneNumber)
    {
        String regex = "((\\+*)((0[ -]*)*|((91 )*))((\\d{12})+|(\\d{10})+))|\\d{5}([- ]*)\\d{6}";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(phoneNumber);

        return m.matches();
    }

    public boolean checkEmail(String email)
    {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(email);

        return m.matches();
    }

    public boolean checkContact(Contact_json contact_json)
    {
        Filters filters = new Filters();

        if(contact_json.contact_name == null && contact_json.contact_email == null && contact_json.contact_phone == null)
        {
            return false;
        }

        if(contact_json.contact_phone != null && !filters.checkPhoneNumber(contact_json.contact_phone))
        {
            return false;
        }

        if(contact_json.contact_email != null && !filters.checkEmail(contact_json.contact_email))
        {
            return false;
        }

        return true;
    }
}
