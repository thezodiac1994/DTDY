package com.example.edexworld.ubhacking;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kosalgeek.asynctask.AsyncResponse;
import com.kosalgeek.asynctask.PostResponseAsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AsyncResponse {

    private static final int CAMERA_REQUEST = 71;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    public static final String ALLOW_KEY = "ALLOWED";
    public static final String CAMERA_PREF = "camera_pref";
    public FirebaseStorage storage;
    public StorageReference storageReference;

    Button btnCamera, uploadImage, getResults;
    ImageView imageView;
    TextView ingredientDesc;
    private Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        btnCamera = (Button)findViewById(R.id.btnCamera);
        uploadImage = (Button)findViewById(R.id.uploadImage);
        getResults = (Button)findViewById(R.id.getResults);
        imageView = (ImageView)findViewById(R.id.imageView);
        ingredientDesc = (TextView)findViewById(R.id.ingredientDesc);

        btnCamera.setOnClickListener(this);
        uploadImage.setOnClickListener(this);
        getResults.setOnClickListener(this);


        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void chooseImage() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new
                        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }

        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"foodImage.jpg");

        filePath = Uri.fromFile(mypath);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK)
        {
            // filePath = Uri.parse("foodImage.jpg");
            Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            saveToInternalStorage(bitmap);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void uploadImage() {
        Log.e("filepath", filePath.toString());
        if(filePath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            //String specialfileName = UUID.randomUUID().toString();
            String specialfileName = "foodImage.jpg";

            StorageReference ref = storageReference.child("images/"+ specialfileName);
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    public void getResults() throws JSONException {

        PostResponseAsyncTask task = new PostResponseAsyncTask(MainActivity.this);
        task.execute("http://10.0.2.2:8080");

        /*String jsonString = "[\n" +
                "  {\n" +
                "    \"total_price\": 16.25,\n" +
                "    \"preptime\": {\n" +
                "      \"min\": 15,\n" +
                "      \"max\": 0\n" +
                "    },\n" +
                "    \"id\": 19315,\n" +
                "    \"ingredients\": [\n" +
                "      {\n" +
                "        \"quantity\": \"1 pkg (8.8 oz)\",\n" +
                "        \"price\": 2.99,\n" +
                "        \"displayOrder\": 1,\n" +
                "        \"name\": \"Food You Feel Good About Traditional Naan Flat Bread\",\n" +
                "        \"sku\": 18155\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"6 Tbsp\",\n" +
                "        \"price\": 2.99,\n" +
                "        \"displayOrder\": 2,\n" +
                "        \"name\": \"Wegmans Organic Chunky Blue Cheese Dressing\",\n" +
                "        \"sku\": 41230\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 cup plus 2 Tbsp\",\n" +
                "        \"price\": 1.99,\n" +
                "        \"displayOrder\": 3,\n" +
                "        \"name\": \"Wegmans Shredded Part-Skim Mozzarella (Dairy Dept), divided\",\n" +
                "        \"sku\": 33435\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 cup crumbled\",\n" +
                "        \"price\": 3.99,\n" +
                "        \"displayOrder\": 4,\n" +
                "        \"name\": \"cooked ground beef\",\n" +
                "        \"sku\": 11914\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1/2 cup\",\n" +
                "        \"price\": \"0.00\",\n" +
                "        \"displayOrder\": 5,\n" +
                "        \"name\": \"Caramelized Onions (see related recipe)\",\n" +
                "        \"sku\": \"None\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"6 slices\",\n" +
                "        \"price\": 4.29,\n" +
                "        \"displayOrder\": 6,\n" +
                "        \"name\": \"Wegmans Fully Cooked Bacon, cut in 1-inch pieces\",\n" +
                "        \"sku\": 33132\n" +
                "      }\n" +
                "    ],\n" +
                "    \"nutrition\": {\n" +
                "      \"information\": \"Each serving (1/2 pizza) contains 630 calories, 35 g carbohydrate, (1 g fiber), 30 g protein, 41 g fat,  (17 g saturated fat), 110 mg cholesterol, and 1080 mg sodium.\",\n" +
                "      \"calories\": 630,\n" +
                "      \"carbohydrates\": 35,\n" +
                "      \"cholesterol\": 110,\n" +
                "      \"saturatedFat\": 17,\n" +
                "      \"protein\": 30,\n" +
                "      \"servingSize\": \"1/2 pizza\",\n" +
                "      \"fat\": 41,\n" +
                "      \"sodium\": 1080\n" +
                "    },\n" +
                "    \"name\": \"Bacon & Blue Cheeseburger Naan Pizza\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"total_price\": 15.99,\n" +
                "    \"preptime\": {\n" +
                "      \"min\": 30,\n" +
                "      \"max\": 0\n" +
                "    },\n" +
                "    \"id\": 6768,\n" +
                "    \"ingredients\": [\n" +
                "      {\n" +
                "        \"quantity\": \"4  (5-6 oz each)\",\n" +
                "        \"price\": 9,\n" +
                "        \"displayOrder\": 1,\n" +
                "        \"name\": \"cold water lobster tails\",\n" +
                "        \"sku\": 509687\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"4 Tbsp\",\n" +
                "        \"price\": 6.99,\n" +
                "        \"displayOrder\": 2,\n" +
                "        \"name\": \"Wegmans Organic Basting Oil, divided\",\n" +
                "        \"sku\": 42994\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": null,\n" +
                "        \"price\": \"0.00\",\n" +
                "        \"displayOrder\": 3,\n" +
                "        \"name\": \"Salt to taste\",\n" +
                "        \"sku\": \"None\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"nutrition\": {\n" +
                "      \"information\": \"Each serving (1 lobster tail) contains 200 calories, 0 g carbohydrate, (0 g fiber), 16 g protein, 15 g fat,  (2 g saturated fat), 125 mg cholesterol, and 420 mg sodium.\",\n" +
                "      \"calories\": 200,\n" +
                "      \"carbohydrates\": 0,\n" +
                "      \"cholesterol\": 125,\n" +
                "      \"saturatedFat\": 2,\n" +
                "      \"protein\": 16,\n" +
                "      \"servingSize\": \"1 lobster tail\",\n" +
                "      \"fat\": 15,\n" +
                "      \"sodium\": 420\n" +
                "    },\n" +
                "    \"name\": \"Classic Baked Lobster Tails\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"total_price\": 26.499999999999996,\n" +
                "    \"preptime\": {\n" +
                "      \"min\": 40,\n" +
                "      \"max\": 0\n" +
                "    },\n" +
                "    \"id\": 21388,\n" +
                "    \"ingredients\": [\n" +
                "      {\n" +
                "        \"quantity\": \"1  (4-5 lbs)\",\n" +
                "        \"price\": 7.49,\n" +
                "        \"displayOrder\": 1,\n" +
                "        \"name\": \"Wegmans Boneless Beef Brisket\",\n" +
                "        \"sku\": 649061\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 tsp\",\n" +
                "        \"price\": 1.99,\n" +
                "        \"displayOrder\": 2,\n" +
                "        \"name\": \"Wegmans Coarse Crystals Sea Salt\",\n" +
                "        \"sku\": 11234\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 1/2 tsp\",\n" +
                "        \"price\": 2.29,\n" +
                "        \"displayOrder\": 3,\n" +
                "        \"name\": \"coarsely ground pepper\",\n" +
                "        \"sku\": 601427\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 Tbsp\",\n" +
                "        \"price\": 1.99,\n" +
                "        \"displayOrder\": 4,\n" +
                "        \"name\": \"Food You Feel Good About Vegetable Oil\",\n" +
                "        \"sku\": 31197\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 1/2 cups chopped\",\n" +
                "        \"price\": 1.5,\n" +
                "        \"displayOrder\": 5,\n" +
                "        \"name\": \"celery\",\n" +
                "        \"sku\": 719590\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 pkg (16 oz)\",\n" +
                "        \"price\": 0.99,\n" +
                "        \"displayOrder\": 6,\n" +
                "        \"name\": \"Food You Feel Good About Baby Cut Carrots\",\n" +
                "        \"sku\": 13763\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 container (7 oz)\",\n" +
                "        \"price\": 2.5,\n" +
                "        \"displayOrder\": 7,\n" +
                "        \"name\": \"Food You Feel Good About Chopped Onions\",\n" +
                "        \"sku\": 32123\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1  (about 8 oz)\",\n" +
                "        \"price\": 1.29,\n" +
                "        \"displayOrder\": 8,\n" +
                "        \"name\": \"turnip, peeled, quartered, cut in 1/2-inch slices\",\n" +
                "        \"sku\": 92642\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1  (about 10 oz)\",\n" +
                "        \"price\": 1.99,\n" +
                "        \"displayOrder\": 9,\n" +
                "        \"name\": \"parsnip, peeled, halved lengthwise, cut in 1/2-inch slices\",\n" +
                "        \"sku\": 92644\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 can (12 oz)\",\n" +
                "        \"price\": 0.99,\n" +
                "        \"displayOrder\": 10,\n" +
                "        \"name\": \"Wegmans Tomato Paste\",\n" +
                "        \"sku\": 44188\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 container (32 oz)\",\n" +
                "        \"price\": 2.19,\n" +
                "        \"displayOrder\": 11,\n" +
                "        \"name\": \"Food You Feel Good About Beef Culinary Stock\",\n" +
                "        \"sku\": 33879\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 cup\",\n" +
                "        \"price\": \"0.00\",\n" +
                "        \"displayOrder\": 12,\n" +
                "        \"name\": \"red wine\",\n" +
                "        \"sku\": \"None\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"1 1/2 tsp chopped\",\n" +
                "        \"price\": 1.29,\n" +
                "        \"displayOrder\": 13,\n" +
                "        \"name\": \"Food You Feel Good About Thyme leaves\",\n" +
                "        \"sku\": 10778\n" +
                "      },\n" +
                "      {\n" +
                "        \"quantity\": \"2 Tbsp\",\n" +
                "        \"price\": \"0.00\",\n" +
                "        \"displayOrder\": 14,\n" +
                "        \"name\": \"Gluten-Free Pan-Searing Flour (see related recipe)\",\n" +
                "        \"sku\": \"None\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"nutrition\": {\n" +
                "      \"information\": \"Each serving (about 5 oz meat, 3/4 cup veg, 1/2 cup sauce) contains 590 calories, 19 g carbohydrate, (4 g fiber), 41 g protein, 37 g fat,  (14 g saturated fat), 135 mg cholesterol, and 480 mg sodium.\",\n" +
                "      \"calories\": 590,\n" +
                "      \"carbohydrates\": 19,\n" +
                "      \"cholesterol\": 135,\n" +
                "      \"saturatedFat\": 14,\n" +
                "      \"protein\": 41,\n" +
                "      \"servingSize\": \"about 5 oz meat, 3/4 cup veg, 1/2 cup sauce\",\n" +
                "      \"fat\": 37,\n" +
                "      \"sodium\": 480\n" +
                "    },\n" +
                "    \"name\": \"Gluten-Free Braised Beef Brisket\"\n" +
                "  }\n" +
                "]";*/
        // alert.setTitle(jsonObj.get("name").toString());
        // alert.setMessage("Message");

        // alert.show();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnCamera:
                chooseImage();
                break;
            case R.id.uploadImage:
                uploadImage();
                break;
            case R.id.getResults:
                try {
                    getResults();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

        }
    }

    @Override
    public void processFinish(String jsonString) {

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            StringBuilder food = new StringBuilder();
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                JSONObject nutrition = jsonObj.getJSONObject("nutrition");
                JSONObject preptime = jsonObj.getJSONObject("preptime");
                JSONArray ingreds = jsonObj.getJSONArray("ingredients");
                String headerAlign = "center";
                String ingredientDescription = "<div align="+headerAlign+">" +
                        "<h2>"+jsonObj.get("name").toString()+"</h2>" +
                        "</div><br />" +
                        "<div><h3>Nutrition:</h3>" +
                        "<table><tr><td>SaturatedFat: </td><td>"+nutrition.get("saturatedFat")+"</td></tr><br />"+
                        "<tr><td>Calories: </td><td>"+nutrition.get("calories")+"</td></tr><br />"+
                        "<tr><td>Protein: </td><td>"+nutrition.get("protein")+"</td></tr><br />"+
                        "<tr><td>Fat: </td><td>"+nutrition.get("fat")+"</td></tr><br />"+
                        "<tr><td>Cholesterol: </td><td>"+nutrition.get("cholesterol")+"</td></tr><br />"+
                        "<tr><td>Sodium: </td><td>"+nutrition.get("sodium")+"</td></tr><br />"+
                        "<tr><td>Carbohydrates: </td><td>"+nutrition.get("carbohydrates")+"</td></tr><br />"+
                        "<tr><td>Serving Size: </td><td>"+nutrition.get("servingSize")+"</td></tr><br />"+
                        "</table></div><br />" +
                        "<div><h3>Preptime:</h3>" +
                        "<table><tr><td>Minimum Prep Time: </td><td>"+preptime.get("min")+" min</td></tr><br />"+
                        "</table></div><br />";
                ingredientDescription+="<div><h3>Ingredients: </h3><table>";
                for(int j=0; j<ingreds.length(); j++) {
                    JSONObject ingred = ingreds.getJSONObject(j);
                    ingredientDescription +="<tr><td>"+ingred.get("name")+": </td>" +
                            "<td>"+ingred.get("quantity")+" </td>" +
                            "<td>$"+ingred.get("price")+" </td>" +
                            "</tr><br />";
                }
                ingredientDescription+="</table></div><br />";
                food.append(ingredientDescription);
            }

            ingredientDesc.setText(Html.fromHtml(food.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}