package com.example.ticketingpos.ticket.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.ticketingpos.MainActivity;
import com.example.ticketingpos.R;
import com.example.ticketingpos.service.ApiService;
import com.example.ticketingpos.ticket.model.MyDatabase;
import com.example.ticketingpos.ticket.model.PriceRequest;
import com.example.ticketingpos.ticket.model.TicketRequest;
import com.example.ticketingpos.ticket.model.Ticket;
import com.example.ticketingpos.ticket.model.TicketResponse;
import com.example.ticketingpos.ticket.model.TicketType;
import com.example.ticketingpos.ticket.restapi.TicketInterface;
import com.google.android.material.textfield.TextInputEditText;
import com.rey.material.widget.CheckBox;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MobileTicketActivity extends AppCompatActivity {
    private String phoneNo,ticketType, adultNo, childNo, saveCurrentDate, saveCurrentTime;
    private int ticketTypeId;
    private TextInputEditText tfAdultNo,tfChildrenNo, tfPhoneNo;
    private android.widget.TextView totalPrice;
    private ImageView imgBack;
    private AutoCompleteTextView tfTicketType;
    private ArrayList<String> getTicketType = new ArrayList<>();
    private CheckBox chkMultiple;
    private Button btnPrice, btnPurchase;
    Dialog dialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_ticket);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        dialog = new Dialog(MobileTicketActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait2);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        totalPrice = findViewById(R.id.tv_total_price);
        imgBack = findViewById(R.id.img_back_mno);
        tfPhoneNo = findViewById(R.id.tf_phone_number);
        tfAdultNo = findViewById(R.id.tf_adult_no);
        tfChildrenNo = findViewById(R.id.tf_child_no);
        tfTicketType = findViewById(R.id.tf_ticket_type);
        btnPrice = findViewById(R.id.btn_get_price);
        chkMultiple = findViewById(R.id.chk_multiple);
        btnPurchase = findViewById(R.id.btn_purchase);
        btnPrice.setOnClickListener((View view)->{validateInput();});
        btnPurchase.setOnClickListener((View view)->{
            validatePurchaseInput();
        });
        btnPurchase.setEnabled(false);
        imgBack.setOnClickListener((View view)->{
            Intent intent = new Intent(MobileTicketActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
        chkMultiple.setChecked(true);
        getTicketType();
    }


    private void validatePurchaseInput() {

        phoneNo = tfPhoneNo.getText().toString();
        ticketType = tfTicketType.getText().toString();
        adultNo = tfAdultNo.getText().toString();
        childNo = tfChildrenNo.getText().toString();

        if (TextUtils.isEmpty(phoneNo))
        {
            Toast.makeText(this, "Please Enter Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(ticketType))
        {
            Toast.makeText(this, "Please Select Ticket Type", Toast.LENGTH_SHORT).show();
        }
        else if(childNo.equals("0") && adultNo.equals("0"))
        {
            Toast.makeText(this, "Please Enter Adult or Children Number", Toast.LENGTH_SHORT).show();
        }
        else
        {
            buyTicket(getMNOTicket());
            dialog = new Dialog(MobileTicketActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_wait2);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }


    private void buyTicket(TicketRequest ticketRequest) {
        Call<TicketResponse> call = ApiService.getTicketApiService().getMNOTicket(ticketRequest);
        call.enqueue(new Callback<TicketResponse>() {
            @Override
            public void onResponse(Call<TicketResponse> call, Response<TicketResponse> response) {
                if (response.isSuccessful()) {
                    TicketResponse ticketResponse = response.body();

                   if (ticketResponse.getBillerResponse() != null && !ticketResponse.getBillerResponse().isEmpty() && ticketResponse.getBillerResponseCode() != null) {
                        for (int i = 0; i < ticketResponse.getBillerResponse().size(); i++) {
//                            Toast.makeText(MobileTicketActivity.this, ticketResponse.getBillerResponse().get(i) + ticketResponse.getBillerResponseCode().get(i), Toast.LENGTH_LONG).show();
                            AlertDialog.Builder builder = new AlertDialog.Builder(MobileTicketActivity.this);
                            builder.setTitle("Transaction Status");
                            builder.setMessage(ticketResponse.getBillerResponse().get(i) + ticketResponse.getBillerResponseCode().get(i));
                            builder.setPositiveButton("Okay", (dialog, which) -> {
                                dialog.dismiss();
                            });
                            builder.show();
                        }
                    }else {
                        List<Ticket> tickets = ticketResponse.getTicket();
                        // Iterate through the list of tickets and log each one

                        for (Ticket ticket : tickets) {

                            int ticketId = ticket.getId();
                            String barcode = ticket.getBarcode();
                            String ticketType = ticket.getTicketName();
                            String ticketNumber = ticket.getTicketNumber();
                            double price = ticket.getTotalPrice();
                            double totalPrice = ticket.getTotalPrice();
                            String childrenSlot = ticket.getChildren();
                            String adultSlot = ticket.getAdult();
                            String phoneNo = tfPhoneNo.getText().toString();
                            String status = "First Print";
                            int print_no = 1;
                            String method = "Mobile Money";
                            //


                            if(chkMultiple.isChecked())
                            {
                                Calendar calendar = Calendar.getInstance();
                                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                Date date = new Date();
                                String new_date = formatter.format(date);
                                MyDatabase myDB = new MyDatabase(MobileTicketActivity.this);
                                myDB.saveTransaction(String.valueOf(ticketId), barcode,  ticketType, ticketNumber,Integer.parseInt(childrenSlot), Integer.parseInt(adultSlot), String.valueOf(totalPrice), method, new_date, print_no, status);
                                Intent intent = new Intent(MobileTicketActivity.this, MultipleTicketActivity.class);
                                intent.putExtra("paymentMethod", method);
                                intent.putExtra("tickets", (Serializable) tickets);
                                startActivity(intent);
                                finish();

                            }else
                            {
                                Calendar calendar = Calendar.getInstance();
                                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                Date date = new Date();
                                String new_date = formatter.format(date);
                                MyDatabase myDB = new MyDatabase(MobileTicketActivity.this);
                                myDB.saveTransaction(String.valueOf(ticketId), barcode,  ticketType, ticketNumber,Integer.parseInt(childrenSlot), Integer.parseInt(adultSlot), String.valueOf(price), method, new_date, print_no,status);

                                //
                                Intent intent = new Intent(MobileTicketActivity.this, TicketDetailActivity.class);
                                intent.putExtra("ticketId", String.valueOf(ticketId));
                                intent.putExtra("barcode", barcode);
                                intent.putExtra("ticketType", ticketType);
                                intent.putExtra("phoneNo", phoneNo);
                                intent.putExtra("ticketNumber", ticketNumber);
                                intent.putExtra("ticketPrice",String.valueOf(price));
                                intent.putExtra("childrenSlot", childrenSlot);
                                intent.putExtra("adultSlot", adultSlot);
                                intent.putExtra("paymentMethod", method);
                                intent.putExtra("status", status);
                                intent.putExtra("print_no", print_no);
                                intent.putExtra("MNO","MNO");
                                startActivity(intent);
                                finish();
                            }

                        }
                        Toast.makeText(MobileTicketActivity.this, "Ticket Purchased", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                    dialog.dismiss();

                } else {
                    // Handle error response
                    Toast.makeText(MobileTicketActivity.this, "Request Failed", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

            }

            @Override
            public void onFailure(Call<TicketResponse> call, Throwable t) {
                Toast.makeText(MobileTicketActivity.this, "Request Failed" +t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }



    private TicketRequest getMNOTicket() {
        int adult, child;
        TicketRequest purchaseRequest = new TicketRequest();
        if(Objects.requireNonNull(tfAdultNo.getText()).toString().equals("")) {
            adult = 0;
            purchaseRequest.setEmail("");
            child = Integer.parseInt(tfChildrenNo.getText().toString());
            purchaseRequest.setTickettypeid(ticketTypeId);
            purchaseRequest.setAdult(adult);
            purchaseRequest.setChildren(child);
            purchaseRequest.setPhoneNumber(tfPhoneNo.getText().toString());
            if(chkMultiple.isChecked()){
                purchaseRequest.setMultiple(true);
            }
            else
            {
                purchaseRequest.setMultiple(false);
            }
        }else if(Objects.requireNonNull(tfChildrenNo.getText()).toString().equals("")) {
            child = 0;
            purchaseRequest.setEmail("");
            adult = Integer.parseInt(tfAdultNo.getText().toString());
            purchaseRequest.setTickettypeid(ticketTypeId);
            purchaseRequest.setAdult(adult);
            purchaseRequest.setChildren(child);
            purchaseRequest.setPhoneNumber(tfPhoneNo.getText().toString());
            if(chkMultiple.isChecked()){
                purchaseRequest.setMultiple(true);
            }
            else
            {
                purchaseRequest.setMultiple(false);
            }
        }else{
            purchaseRequest.setEmail("");
            adult = Integer.parseInt(tfAdultNo.getText().toString());
            child = Integer.parseInt(tfChildrenNo.getText().toString());
            purchaseRequest.setTickettypeid(ticketTypeId);
            purchaseRequest.setAdult(adult);
            purchaseRequest.setChildren(child);
            purchaseRequest.setPhoneNumber(tfPhoneNo.getText().toString());
            if(chkMultiple.isChecked()){
                purchaseRequest.setMultiple(true);
            }
            else
            {
                purchaseRequest.setMultiple(false);
            }
        }

        return purchaseRequest;
    }


    private void validateInput()
    {
        phoneNo = tfPhoneNo.getText().toString();
        ticketType = tfTicketType.getText().toString();
        adultNo = tfAdultNo.getText().toString();
        childNo = tfChildrenNo.getText().toString();
        if (TextUtils.isEmpty(phoneNo))
        {
            Toast.makeText(this, "Please Enter Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(ticketType))
        {
            Toast.makeText(this, "Please Select Ticket Type", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(adultNo) && TextUtils.isEmpty(childNo))
        {
            Toast.makeText(this, "Please Enter Adult or Children Number", Toast.LENGTH_SHORT).show();
        }
        else if(childNo.equals("0") && adultNo.equals("0"))
        {
            Toast.makeText(this, "Please Enter Adult or Children Number", Toast.LENGTH_SHORT).show();
        }
        else
        {
            dialog = new Dialog(MobileTicketActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_wait2);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            getPrice(getQuote());
        }

    }

    private void getPrice(PriceRequest priceRequest) {
        Call<PriceRequest> priceRequestCall = ApiService.getTicketApiService().getPrice(priceRequest);
        priceRequestCall.enqueue(new Callback<PriceRequest>() {
            @Override
            public void onResponse(Call<PriceRequest> call, Response<PriceRequest> response) {
                if(response.isSuccessful())
                {
                    // Get the response body as a string
                    PriceRequest priceTotal = response.body();
                    double total = priceTotal.getPrice();
                    String ans;
                    ans = String.valueOf(total);
                    totalPrice.setText(ans);
                    dialog.dismiss();
                    btnPurchase.setEnabled(true);
                }
                else
                {
                    Toast.makeText(MobileTicketActivity.this, "Request Failed", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<PriceRequest> call, Throwable t) {
                Toast.makeText(MobileTicketActivity.this, "Request Failed" +t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    public PriceRequest getQuote()
    {
        PriceRequest priceRequest = new PriceRequest();
        String adult;
        String child;
        if(Objects.requireNonNull(tfAdultNo.getText()).toString().equals("")) {
            adult = "0";
            child = tfChildrenNo.getText().toString();
            priceRequest.setId(ticketTypeId);
            priceRequest.setAdult(Integer.parseInt(adult));
            priceRequest.setChildren(Integer.parseInt(child));
        }else if(Objects.requireNonNull(tfChildrenNo.getText()).toString().equals("")) {
            child = "0";
            adult = tfAdultNo.getText().toString();
            priceRequest.setId(ticketTypeId);
            priceRequest.setAdult(Integer.parseInt(adult));
            priceRequest.setChildren(Integer.parseInt(child));
        }else{
            adult = tfAdultNo.getText().toString();
            child = tfChildrenNo.getText().toString();
            priceRequest.setId(ticketTypeId);
            priceRequest.setAdult(Integer.parseInt(adult));
            priceRequest.setChildren(Integer.parseInt(child));
        }

        return priceRequest;
    }

    private void getTicketType()
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TicketInterface.base_url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        TicketInterface api = retrofit.create(TicketInterface.class);
        Call<String> call = api.getTicketType();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.i("Response", response.body());
                if(response.isSuccessful()){
                    if(response.body()!=null){
                        Log.i("Success", response.body());
                        try {
                            String getResponse = response.body();
                            List<TicketType> getTicketTypeData = new ArrayList<TicketType>();
                            JSONObject object = new JSONObject(getResponse);
                            JSONArray array  = object.getJSONArray("types");
                            getTicketTypeData.add(new TicketType(-1, "---Select---"));

                            for (int i = 0; i < array.length(); i++)
                            {
                                TicketType ticket = new TicketType();
                                JSONObject JsonObject = array.getJSONObject(i);
                                ticket.setId(JsonObject.getInt("id"));
                                ticket.setName(JsonObject.getString("name"));
                                getTicketTypeData.add(ticket);
                            }
                            for(int i=0; i<getTicketTypeData.size(); i++){
                                getTicketType.add(getTicketTypeData.get(i).getName());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MobileTicketActivity.this, R.layout.spinner_item, getTicketType);

                            adapter.setDropDownViewResource(R.layout.spinner_item);
                            tfTicketType.setAdapter(adapter);

                            tfTicketType.setOnItemClickListener((parent, view, position, id) -> {
                                String selectedItem = (String) parent.getItemAtPosition(position);
                                // Get the index number of the selected item
                                int index = getTicketType.indexOf(selectedItem);
                                // You can now use the "index" variable to get the item from the JSON array
                                Log.d("item selected", String.valueOf(index));

                                ticketTypeId = index;

                                // You can now use the index variable to access the selected item in the JSON array
                            });
                        }catch (JSONException ex){
                            ex.printStackTrace();
                        }
                        dialog.dismiss();
                        Toast.makeText(MobileTicketActivity.this, "Data Loaded Successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MobileTicketActivity.this, "Request Failed" +t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }
}