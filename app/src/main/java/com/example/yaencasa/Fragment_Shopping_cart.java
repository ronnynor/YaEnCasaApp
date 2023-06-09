package com.example.yaencasa;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yaencasa.Adapters.AdapterR_Shopping_cart;
import com.example.yaencasa.Auxiliary.Constants;
import com.example.yaencasa.Auxiliary.IDCreater;
import com.example.yaencasa.Auxiliary.NetworkTools;
import com.example.yaencasa.Data.Cart_Elements;
import com.example.yaencasa.Data.ModelElement;
import com.example.yaencasa.Data.ModelProduct;
import com.example.yaencasa.Data.ModelZone;
import com.example.yaencasa.Data.Network.RetrofitOrdersImpl;
import com.example.yaencasa.Data.Network.RetrofitZoneslmpl;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.shashank.sony.fancytoastlib.FancyToast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Fragment_Shopping_cart extends Fragment {

    //Recycler
    private AdapterR_Shopping_cart adapterR_shopping_cart;
    private RecyclerView recycler;
    private ArrayList<ModelElement> al_shopping_cart;
    private ArrayList<ModelProduct> al_products;
    private ConstraintLayout linearLayoutEmpty;


    //Details sending
    private EditText cell_num;
    private EditText ET_adress;
    private double priceTotalCUP;
    private EditText ET_name;
    private ProgressDialog progressDialogUp;
    private TextInputLayout textInputLayout;
    private AutoCompleteTextView autoC;
    private String selectedZone="No";
    private int lastAutoCSelected=0;
    private Button showMap;
    private ExtendedFloatingActionButton sendOrder;


    //Shared Preferences
    private boolean show=true;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedEditor;

    //Location
    private static final int RESULT_CODE_MAP = 31;
    private String latitude = "no";
    private String longitude = "no";

    //Internet
    private RetrofitOrdersImpl retrofitOrders;
    private ProgressDialog progressDialogSubiend;
    private TextView tv_totalPrice;

    //Zone
    private ProgressDialog progressDialogCargando;
    private ArrayList<ModelZone> al_zones;
    private ArrayList<String> al_zones_str;
    private RetrofitZoneslmpl retrofitZoneslmpl;

    public Fragment_Shopping_cart() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root=inflater.inflate(R.layout.fragment_shopping_cart,container,false);

        //Declarations
        cell_num=(EditText) root.findViewById(R.id.FSC_ET_cellNum);
        ET_adress=(EditText) root.findViewById(R.id.FSC_ET_adress);
        LinearLayout linearLayoutSheet= root.findViewById(R.id.FSC_LL_personal_info);
        ET_name=(EditText) root.findViewById(R.id.FSC_ET_name);
        textInputLayout=(TextInputLayout)root.findViewById(R.id.FSC_TIL_spinner);
        autoC=(AutoCompleteTextView) root.findViewById(R.id.FSC_autoC);
        showMap=(Button) root.findViewById(R.id.FSC_Btn_Ubic);
        showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                click_choice_loc();
            }
        });
        sendOrder = (ExtendedFloatingActionButton) root.findViewById(R.id.FSC_FAB_sendOrder);
        sendOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_enviarPedido();
            }
        });

        //Spinner--Zona
        al_zones = new ArrayList<>();
        al_zones_str = new ArrayList<>();


        //RecyclerView
        tv_totalPrice = (TextView) root.findViewById(R.id.FSC_PrecioTotal);
        al_products = new ArrayList<>();
        recycler = (RecyclerView) root.findViewById(R.id.FSC_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler.setHasFixedSize(true);
        al_shopping_cart = Cart_Elements.al_elements;
        adapterR_shopping_cart = new AdapterR_Shopping_cart(getContext(), al_shopping_cart);
        recycler.setAdapter(adapterR_shopping_cart);
        linearLayoutEmpty=(ConstraintLayout) root.findViewById(R.id.FSC_CL_empty);


        //Preferencias
        sharedPreferences = requireActivity().getSharedPreferences("YaEnCasa", 0);
        sharedEditor= sharedPreferences.edit();
        show=sharedPreferences.getBoolean("mostrar", true);
        ET_name.setText(sharedPreferences.getString("Nombre", ""));
        cell_num.setText(sharedPreferences.getString("NumTelef", ""));
        ET_adress.setText(sharedPreferences.getString("Direccion", ""));
        int index = sharedPreferences.getInt("zoneIndexS", 0);
        if (autoC.getText().toString().trim().isEmpty()) {
            selectedZone = "No";
        } else {
            selectedZone = autoC.getText().toString();
        }

        //Zones
        al_zones = new ArrayList<>();

        //Internet
        retrofitOrders = new RetrofitOrdersImpl();
        retrofitZoneslmpl = new RetrofitZoneslmpl();

        loadMainInternetInfo();


        return root;
    }


    //Start
    private void loadMainInternetInfo() {
        if (NetworkTools.isOnline(requireContext(), false)) {
            progressDialogCargando = ProgressDialog.show(requireContext(), getString(R.string.Cargando_datos), getString(R.string.Espere), false, false);
            loadZone();
        } else {
            showAlertDialogNoInternet();
        }
    }

    private void loadZone() {
        if (NetworkTools.isOnline(requireContext(), false)) {

            Call<ArrayList<ModelZone>> call = retrofitZoneslmpl.fetchZones();
            call.enqueue(new Callback<ArrayList<ModelZone>>() {
                @Override
                public void onResponse(Call<ArrayList<ModelZone>> call, Response<ArrayList<ModelZone>> response) {
                    if (response.isSuccessful()) {
                        progressDialogCargando.dismiss();
                        al_zones = response.body();
                        makeZonesStr();
                    } else {
                        progressDialogCargando.dismiss();
                        NetworkTools.showAlertDialogNoInternet(requireContext());
                    }
                }

                @Override
                public void onFailure(Call<ArrayList<ModelZone>> call, Throwable t) {
                    progressDialogCargando.dismiss();
                    NetworkTools.showAlertDialogNoInternet(requireContext());
                }
            });


        } else {
            showAlertDialogNoInternet();
        }

    }

    private void makeZonesStr() {
        for (ModelZone model : al_zones) {
            al_zones_str.add(model.getName() + "---------" + model.getPrice() + " CUP");
        }
        updateRecyclerAdapter();
    }

    private void updateRecyclerAdapter() {
        //Cart adapter
        adapterR_shopping_cart = new AdapterR_Shopping_cart(getContext(), al_shopping_cart);
        if (al_shopping_cart.isEmpty()) {
            linearLayoutEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.INVISIBLE);
        } else {
            linearLayoutEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
        adapterR_shopping_cart.setClickListener(new AdapterR_Shopping_cart.RecyclerTouchListener() {
            @Override
            public void onClickItem(View v, int position) {
                showAlertDialogDelete(position);
            }
        });
        recycler.setAdapter(adapterR_shopping_cart);

        //Zones Adapter
        ArrayAdapter<String> strigArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_items_spinner, al_zones_str);
        autoC.setAdapter(strigArrayAdapter);
        autoC.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (autoC.getText().toString().trim().isEmpty()) {
                    selectedZone = "No";
                } else {
                    selectedZone = autoC.getText().toString();
                    lastAutoCSelected = i;
                }
            }
        });

        makeTotalPrice();
    }

    private void makeTotalPrice() {
        priceTotalCUP = 0;
        for (ModelElement element : al_shopping_cart) {
            priceTotalCUP += element.getPrice();
        }

        String textTotalPrice = getString(R.string.Precio_total) + " " + priceTotalCUP + " CUP";
        tv_totalPrice.setText(textTotalPrice);
    }

    public void showAlertDialogNoInternet() {
        //init alert dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setCancelable(false);
        builder.setTitle(R.string.error);
        builder.setMessage(R.string.Revise_su_conexion);
        //set listeners for dialog buttons
        builder.setPositiveButton(R.string.Reintentar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                loadMainInternetInfo();
            }
        });

        //create the alert dialog and show it
        builder.create().show();
    }


    //Aux
    public void showAlertDialogDelete(int pos) {
        //init alert dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle(R.string.Eliminar_elementos);
        builder.setMessage(R.string.Tiene_certeza_eliminar_elemento);
        //set listeners for dialog buttons
        builder.setNegativeButton(R.string.No, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.Si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                al_shopping_cart.remove(pos);
                updateRecyclerAdapter();
            }
        });

        //create the alert dialog and show it
        builder.create().show();
    }
    private String makeProduct(){
        StringBuilder stringBuilder=new StringBuilder("");
        /*
        1 Pan con pasta verde
          Cantidad: 4
          Precio: 500 CUP
          Agregos:
            Salsa
            Picante
         */
        for (int a=0;a<al_shopping_cart.size();a++){
            stringBuilder.append((a+1)+" "+al_shopping_cart.get(a).getProduct().getName());
            stringBuilder.append("--n--");
            stringBuilder.append("   Cantidad: "+al_shopping_cart.get(a).getAmount());
            stringBuilder.append("--n--");
            stringBuilder.append("   Precio: ");
            stringBuilder.append(al_shopping_cart.get(a).getPrice());
            stringBuilder.append(" CUP");
            stringBuilder.append("--n--");
        }
        return stringBuilder.toString().replace("*","+");
    }

    //BtnSendOrder
    public void btn_enviarPedido() {
        checkData();
    }
    private void checkData(){
        if (NetworkTools.isOnline(requireContext(), true)) {
            if(!al_shopping_cart.isEmpty()) {
                if (!ET_name.getText().toString().trim().isEmpty()) {
                    if (!cell_num.getText().toString().trim().isEmpty()) {
                        if (!ET_adress.getText().toString().trim().isEmpty()) {
                            if(!longitude.equals("no") && !latitude.equals("no")){
                                if(!selectedZone.equals("No")) {
                                    sharedEditor.putString("Nombre", ET_name.getText().toString());
                                    sharedEditor.putString("NumTelef", cell_num.getText().toString());
                                    sharedEditor.putString("Direccion", ET_adress.getText().toString());
                                    sharedEditor.putInt("zoneIndexS", lastAutoCSelected);
                                    sharedEditor.apply();
                                    showAlertDialogVerifEnvio();
                                }else{
                                    autoC.setError(getString(R.string.Este_campo_no_puede_vacio));
                                }
                            }else{
                                FancyToast.makeText(requireContext(),getString(R.string.selecc_ubic),FancyToast.LENGTH_SHORT, FancyToast.ERROR,false).show();
                            }
                        } else {
                            ET_adress.setError(getString(R.string.Este_campo_no_puede_vacio));
                        }
                    } else {
                        cell_num.setError(getString(R.string.Este_campo_no_puede_vacio));
                    }
                } else {
                    ET_name.setError(getString(R.string.Este_campo_no_puede_vacio));
                }
            }else {
                showAlertDialogEmpty();
            }
        }
    }
    public void showAlertDialogEmpty() {
        //init alert dialog
        android.app.AlertDialog.Builder builder =  new android.app.AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle(R.string.Carrito_vacio);
        builder.setMessage(R.string.Nada_a_enviar);
        //set listeners for dialog buttons
        builder.setPositiveButton(R.string.Aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //create the alert dialog and show it
        builder.create().show();
    }
    private void showAlertDialogVerifEnvio(){
        //init alert dialog
        android.app.AlertDialog.Builder builder =  new android.app.AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle(R.string.Enviar_pedido);
        String message=getString(R.string.verificar_envio);
        builder.setMessage(message);
        //set listeners for dialog buttons
        builder.setPositiveButton(R.string.Aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                progressDialogSubiend=ProgressDialog.show(requireContext(),getString(R.string.Enviando_pedido),getString(R.string.por_favor_espere),false,false);
                sendOrderInternet();
            }
        });
        builder.setNegativeButton(R.string.Cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //create the alert dialog and show it
        builder.create().show();
    }
    private void sendOrderInternet(){
        String token = Constants.PHP_TOKEN;
        double price = priceTotalCUP;
        String products = makeProduct();
        String celnumber = cell_num.getText().toString();
        String location = latitude+":"+longitude;
        String address = ET_adress.getText().toString();
        String name = ET_name.getText().toString();
        String zone = autoC.getText().toString();

        Call<Integer> call = retrofitOrders.addOrder(token, price, products, celnumber, location, address, name, zone, IDCreater.personalId);

        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if(response.isSuccessful()){
                    progressDialogSubiend.dismiss();
                    li_finalizado(response.body());
                }else{
                    progressDialogSubiend.dismiss();
                    NetworkTools.showAlertDialogNoInternet(getContext());
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                progressDialogSubiend.dismiss();
                NetworkTools.showAlertDialogNoInternet(getContext());
            }
        });
    }
    private void li_finalizado(Integer id){
        LayoutInflater inflater=LayoutInflater.from(getContext());
        View vista= inflater.inflate(R.layout.li_pedido_enviado, null);
        androidx.appcompat.app.AlertDialog.Builder builder=new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setView(vista);
        final androidx.appcompat.app.AlertDialog alertDialog=builder.create();

        //Declaraciones
        ImageView imagenCruz=(ImageView)vista.findViewById(R.id.liPE_IV_cerrar);
        CardView cardViewPedidos=(CardView)vista.findViewById(R.id.liPE_CV_ir_a_pedidos);
        CardView cardViewWapp=(CardView)vista.findViewById(R.id.liPE_CV_ir_a_Wapp);

        //Listener
        imagenCruz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        cardViewPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                ir_a_mis_pedidos();
            }
        });


        //Preferernces
        al_shopping_cart.clear();
        updateRecyclerAdapter();

        //Finalizado
        builder.setCancelable(true);
        alertDialog.getWindow().setGravity(Gravity.CENTER);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }
    private void ir_a_mis_pedidos(){

        if(NetworkTools.isOnline(requireContext(),true)){
            Intent intent=new Intent(getContext(),Activity_MyOrders.class);
            startActivity(intent);
        }
    }


    public void click_choice_loc() {
        if (NetworkTools.isOnline(requireContext(), true)) {
            startActivityForResult(new Intent(requireActivity(), Activity_PutMap.class), 31);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CODE_MAP) {
            if (resultCode == Activity.RESULT_OK) {
                latitude = data.getStringExtra("latitude");
                longitude = data.getStringExtra("longitude");
                showMap.setText(getString(R.string.change_location));
                FancyToast.makeText(requireContext(), getString(R.string.Ubicacion_obtenida_con_exito), FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
            }else{
                FancyToast.makeText(requireContext(), getString(R.string.error_get_location), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
            }
        }
    }
}