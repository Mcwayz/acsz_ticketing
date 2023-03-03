package com.example.ticketingpos.ticket.activity;

import static com.ftpos.library.smartpos.errcode.ErrCode.ERR_SUCCESS;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_CENTER;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_LEFT;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_RIGHT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.ticketingpos.R;
import com.example.ticketingpos.ticket.adapter.CustomAdapter;
import com.example.ticketingpos.ticket.adapter.TicketAdapter;
import com.example.ticketingpos.ticket.model.MyDatabase;
import com.example.ticketingpos.ticket.model.Ticket;
import com.example.ticketingpos.ticket.restapi.RecyclerViewInterface;
import com.ftpos.library.smartpos.printer.OnPrinterCallback;
import com.ftpos.library.smartpos.printer.PrintStatus;
import com.ftpos.library.smartpos.printer.Printer;
import com.ftpos.library.smartpos.servicemanager.OnServiceConnectCallback;
import com.ftpos.library.smartpos.servicemanager.ServiceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MultipleTicketActivity extends AppCompatActivity implements RecyclerViewInterface {
    TicketAdapter ticketAdapter;
    private CustomAdapter customAdapter;
    RecyclerView recyclerView;
    private FloatingActionButton fab;
    private ImageView imgQRC, imgBack;
    private Context mContext;
    private ArrayList<String> txn_id, ticket_id, barcode, ticket_type, ticket_number, children, adult, price, payment, date, status, print_no;
    private String paymentMethod,saveCurrentDate, saveCurrentTime;
    private static final String CHANNEL = "com.example.ticketingpos/printAction";
    private Printer printer;
    private static Paint paint = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_ticket);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fab = findViewById(R.id.fab_print_mul);
        imgQRC = findViewById(R.id.img_hidden_qrc);
        recyclerView = findViewById(R.id.recyclerView1);
        imgBack = findViewById(R.id.img_back_mul);
        mContext = MultipleTicketActivity.this;
        txn_id = new ArrayList<>();
        ticket_id = new ArrayList<>();
        barcode = new ArrayList<>();
        ticket_type = new ArrayList<>();
        ticket_number = new ArrayList<>();
        children = new ArrayList<>();
        adult = new ArrayList<>();
        price = new ArrayList<>();
        date = new ArrayList<>();
        payment = new ArrayList<>();
        status = new ArrayList<>();
        print_no = new ArrayList<>();
        paymentMethod = getIntent().getStringExtra("paymentMethod");
        imgBack.setOnClickListener((View view)->{
            Intent intent = new Intent(MultipleTicketActivity.this, MobileTicketActivity.class);
            startActivity(intent);
        });
        try {
            ServiceManager.bindPosServer(MultipleTicketActivity.this, new OnServiceConnectCallback() {
                @Override
                public void onSuccess() {
                    printer = Printer.getInstance(mContext);
                }
                @Override
                public void onFail(int var1) {
                    Log.e("binding", "onFail");
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getMultiple();

        fab.setOnClickListener((View view)-> printMultipleReceipt());

        MultiFormatWriter write = new MultiFormatWriter();
        try {
            BitMatrix matrix = write.encode(String.valueOf(barcode), BarcodeFormat.QR_CODE, 350, 350);
            BarcodeEncoder encoder = new BarcodeEncoder();
            //Initialize bitmap
            Bitmap bitmap = encoder.createBitmap(matrix);
            imgQRC.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
    private void getMultiple() {
        Intent intent = getIntent();
        ArrayList<Ticket> ticketList = intent.getParcelableArrayListExtra("tickets");
        ticketAdapter = new TicketAdapter(ticketList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(ticketAdapter);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());
        if (ticketList.size() == 0) {
            Toast.makeText(this, "No Tickets Found!", Toast.LENGTH_SHORT).show();
        } else {
            // Add data to the lists
            for (Ticket ticket : ticketList) {
                txn_id.add(String.valueOf(ticket.getId()));
                ticket_id.add(String.valueOf(ticket.getId()));
                barcode.add(ticket.getBarcode());
                ticket_type.add(ticket.getTicketName());
                ticket_number.add(ticket.getTicketNumber());
                children.add(ticket.getChildren());
                adult.add(ticket.getAdult());
                price.add(String.valueOf(ticket.getTotalPrice()));
                date.add(saveCurrentDate);
                print_no.add(getIntent().getStringExtra("print_no"));
                status.add(getIntent().getStringExtra("status"));
                payment.add(getIntent().getStringExtra("paymentMethod"));
                //
                int child = Integer.parseInt(ticket.getChildren());
                int adult = Integer.parseInt(ticket.getAdult());
                String t_id = String.valueOf(ticket.getId());
                String t_number = ticket.getTicketNumber();
                String t_barcode = ticket.getBarcode();
                String t_type = ticket.getTicketName();
                String t_price = String.valueOf(ticket.getTotalPrice());
                String method = String.valueOf(getIntent().getStringExtra("paymentMethod"));
                int print = 1;
                String p_status =  getIntent().getStringExtra("status");
                //
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                Date date = new Date();
                String new_date = formatter.format(date);
                MyDatabase myDB = new MyDatabase(MultipleTicketActivity.this);
                myDB.saveTransaction(t_id,t_barcode,t_type, t_number
                ,child, adult, t_price, method, new_date, print, p_status);

            }
            customAdapter = new CustomAdapter(MultipleTicketActivity.this, txn_id, ticket_id,
            barcode, ticket_type, ticket_number, children, adult,price, date, this);
            recyclerView.setAdapter(customAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(MultipleTicketActivity.this));
        }
    }


    private void printMultipleReceipt() {
        // Extract the details of the ticket
        Intent intent = getIntent();
        ArrayList<Ticket> ticketList = intent.getParcelableArrayListExtra("tickets");

        // Loop through each ticket in the list and print it

        for (Ticket ticket : ticketList) {
            int id = ticket.getId();
            String barcode = ticket.getBarcode();
            String ticketName = ticket.getTicketName();
            String ticketNumber = ticket.getTicketNumber();
            String children = ticket.getChildren();
            String adult = ticket.getAdult();
            double ticketPrice = ticket.getTotalPrice();
            paymentMethod = getIntent().getStringExtra("paymentMethod");

            // Code to create the QR code image
            MultiFormatWriter write = new MultiFormatWriter();
            try {
                BitMatrix matrix = write.encode(barcode, BarcodeFormat.QR_CODE, 350, 350);
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.createBitmap(matrix);
                imgQRC.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
                try {
                    int ret;
                    ret = printer.open();
                    if (ret != ERR_SUCCESS) {
                        System.out.println("open failed" + String.format(" errCode = 0x%x\n", ret));
                        Toast.makeText(MultipleTicketActivity.this, "open failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                        return;
                    }

                    ret = printer.startCaching();
                    if (ret != ERR_SUCCESS) {
                        System.out.println("startCaching failed" + String.format(" errCode = 0x%x\n", ret));
                        Toast.makeText(MultipleTicketActivity.this, "startCaching failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                        return;
                    }

                    ret = printer.setGray(3);
                    if (ret != ERR_SUCCESS) {
                        System.out.println("startCaching failed" + String.format(" errCode = 0x%x\n", ret));
                        Toast.makeText(MultipleTicketActivity.this, "startCaching failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                        return;
                    }

                    PrintStatus printStatus = new PrintStatus();
                    ret = printer.getStatus(printStatus);
                    if (ret != ERR_SUCCESS) {
                        System.out.println("getStatus failed" + String.format(" errCode = 0x%x\n", ret));
                        Toast.makeText(MultipleTicketActivity.this, "getStatus failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                        return;
                    }

                    System.out.println("Temperature = " + printStatus.getmTemperature() + "\n");
                    System.out.println("Gray = " + printStatus.getmGray() + "\n");
                    if (!printStatus.getmIsHavePaper()) {
                        System.out.println("Printer out of paper\n");
                        Toast.makeText(MultipleTicketActivity.this, "Printer Out of Paper", Toast.LENGTH_LONG).show();
                        return;
                    }

                    System.out.println("IsHavePaper = true\n");

                    printer.setAlignStyle(PRINT_STYLE_CENTER);
                    Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.logo);
                    ret = printer.printBmp(bmp);
                    printer.printStr("\n");
                    printer.printStr("__________________________________\n");

                    printer.setAlignStyle(PRINT_STYLE_CENTER);
                    Bitmap qrcode = ((BitmapDrawable) imgQRC.getDrawable()).getBitmap();
                    printer.printBmp(qrcode);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Ticket ID");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(String.valueOf(id));
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Ticket Type");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(ticketName);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Ticket Number");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(ticketNumber);
                    printer.printStr("\n");

                    printer.setAlignStyle(PRINT_STYLE_CENTER);
                    printer.printStr("__________________________________");
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Adult Slots");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(adult);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Children Slots");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(children);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Amount Paid");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr("K" + " " + ticketPrice);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Payment Method");
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(paymentMethod);
                    printer.printStr("\n");

                    printer.setAlignStyle(PRINT_STYLE_CENTER);
                    printer.printStr("__________________________________");
                    printer.printStr("\n");
                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Date");
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
                    saveCurrentDate = currentDate.format(calendar.getTime());
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(saveCurrentDate);
                    printer.printStr("\n");

                    //Single line print left justified, right justified
                    printer.setAlignStyle(PRINT_STYLE_LEFT);
                    printer.printStr("Time");
                    SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
                    saveCurrentTime = currentTime.format(calendar.getTime());
                    printer.setAlignStyle(PRINT_STYLE_RIGHT);
                    printer.printStr(saveCurrentTime);
                    printer.printStr("\n");

                    printer.setAlignStyle(PRINT_STYLE_CENTER);
                    printer.printStr("__________________________________\n");
                    printer.printStr("Thank You");

                    ret = printer.getUsedPaperLenManage();
                    if (ret < 0) {
                        System.out.println("getUsedPaperLenManage failed" + String.format(" errCode = 0x%x\n", ret));
                        Toast.makeText(MultipleTicketActivity.this, "getUsedPaperLenManage failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                    }

                    System.out.println("UsedPaperLenManage = " + ret + "mm \n");
                    printer.print(new OnPrinterCallback() {
                        @Override
                        public void onSuccess() {
                            System.out.println("Print Success\n");
                            printer.feed(32);
                        }

                        @Override
                        public void onError(int i) {

                            System.out.println("printBmp failed" + String.format(" errCode = 0x%x\n", i));
                            Toast.makeText(MultipleTicketActivity.this, "PrintBmp Failed" + String.format(" errCode = 0x%x\n", i), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Print Failed" + e + "\n");
                    Toast.makeText(MultipleTicketActivity.this, "Print Failed" + e, Toast.LENGTH_LONG).show();
                }
        }

    }

    void printOneReceipt(String ticketID, String ticketType, String ticketNumber, String ticketPrice, String childrenSlot, String adultSlot, String paymentMethod, String print_no) {
        try {
            int ret;
            ret = printer.open();
            if (ret != ERR_SUCCESS) {
                System.out.println("open failed" + String.format(" errCode = 0x%x\n", ret));
                Toast.makeText(MultipleTicketActivity.this, "open failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                return;
            }

            ret = printer.startCaching();
            if (ret != ERR_SUCCESS) {
                System.out.println("startCaching failed" + String.format(" errCode = 0x%x\n", ret));
                Toast.makeText(MultipleTicketActivity.this, "startCaching failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                return;
            }

            ret = printer.setGray(3);
            if (ret != ERR_SUCCESS) {
                System.out.println("startCaching failed" + String.format(" errCode = 0x%x\n", ret));
                Toast.makeText(MultipleTicketActivity.this, "startCaching failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                return;
            }

            PrintStatus printStatus = new PrintStatus();
            ret = printer.getStatus(printStatus);
            if (ret != ERR_SUCCESS) {
                System.out.println("getStatus failed" + String.format(" errCode = 0x%x\n", ret));
                Toast.makeText(MultipleTicketActivity.this, "getStatus failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
                return;
            }

            System.out.println("Temperature = " + printStatus.getmTemperature() + "\n");
            System.out.println("Gray = " + printStatus.getmGray() + "\n");
            if (!printStatus.getmIsHavePaper()) {
                System.out.println("Printer out of paper\n");
                Toast.makeText(MultipleTicketActivity.this, "Printer Out of Paper", Toast.LENGTH_LONG).show();
                return;
            }

            System.out.println("IsHavePaper = true\n");

            printer.setAlignStyle(PRINT_STYLE_CENTER);
            Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.logo);
            printer.printBmp(bmp);
            printer.printStr("\n");
            printer.printStr("__________________________________\n");

            printer.setAlignStyle(PRINT_STYLE_CENTER);
            Bitmap qrcode = ((BitmapDrawable) imgQRC.getDrawable()).getBitmap();
            printer.printBmp(qrcode);

            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Ticket ID");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(ticketID);

            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Ticket Type");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(ticketType);

            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Ticket Number");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(ticketNumber);

            printer.printStr("\n");
            printer.setAlignStyle(PRINT_STYLE_CENTER);
            printer.printStr("__________________________________");
            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Adult Slots");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(childrenSlot);

            printer.printStr("\n");
            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Children Slots");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(adultSlot);
            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Amount Paid");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr("K"+" "+ticketPrice);
            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Payment Method");
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(paymentMethod);
            printer.printStr("\n");
            printer.setAlignStyle(PRINT_STYLE_CENTER);
            printer.printStr("__________________________________");
            printer.printStr("\n");
            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Date");
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(saveCurrentDate);
            printer.printStr("\n");

            //Single line print left justified, right justified
            printer.setAlignStyle(PRINT_STYLE_LEFT);
            printer.printStr("Time");
            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
            saveCurrentTime = currentTime.format(calendar.getTime());
            printer.setAlignStyle(PRINT_STYLE_RIGHT);
            printer.printStr(saveCurrentTime);
            printer.printStr("\n");
            printer.setAlignStyle(PRINT_STYLE_CENTER);
            printer.printStr("__________________________________\n");
            printer.printStr(" Thank You");

            ret = printer.getUsedPaperLenManage();
            if (ret < 0) {
                System.out.println("getUsedPaperLenManage failed" + String.format(" errCode = 0x%x\n", ret));
                Toast.makeText(MultipleTicketActivity.this, "getUsedPaperLenManage failed" + String.format(" errCode = 0x%x\n", ret), Toast.LENGTH_LONG).show();
            }

            System.out.println("UsedPaperLenManage = " + ret + "mm \n");

            printer.print(new OnPrinterCallback() {
                @Override
                public void onSuccess() {
                    System.out.println("Print Success\n");
                    printer.feed(32);
                }

                @Override
                public void onError(int i) {
                    System.out.println("printBmp failed" + String.format(" errCode = 0x%x\n", i));
                    Toast.makeText(MultipleTicketActivity.this, "PrintBmp Failed" + String.format(" errCode = 0x%x\n", i), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Print Failed" + e + "\n");
            Toast.makeText(MultipleTicketActivity.this, "Print Failed" + e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(MultipleTicketActivity.this, TicketDetailActivity.class);
        intent.putExtra("ticketId", ticket_id.get(position));
        intent.putExtra("barcode", barcode.get(position));
        intent.putExtra("ticketType", ticket_type.get(position));
        intent.putExtra("ticketNumber", ticket_number.get(position));
        intent.putExtra("ticketPrice",  price.get(position));
        intent.putExtra("childrenSlot", children.get(position));
        intent.putExtra("adultSlot",  adult.get(position));
        intent.putExtra("paymentMethod", payment.get(position));
        intent.putExtra("Print_no", print_no.get(position));
        intent.putExtra("status", "First Print");
        intent.putExtra("History","History");
        //
        printOneReceipt(ticket_id.get(position), ticket_type.get(position),
        ticket_number.get(position),price.get(position), children.get(position),
        adult.get(position),payment.get(position), print_no.get(position));
    }
}