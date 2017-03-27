package com.virtualbienes.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.virtualbienes.app.models.Department;
import com.virtualbienes.app.models.Municipality;
import com.virtualbienes.app.models.Neighborhood;
import com.virtualbienes.app.models.Sector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final String LOG_TAG = this.getClass().getSimpleName();
    private final int LOAD_POLYGON_ASYNCTASK = 0;
    private final int LOAD_DEPARTMENTS_ASYNCTASK = 1;
    private final int LOAD_MUNICIPALITIES_ASYNCTASK = 2;
    private final int LOAD_SECTORS_ASYNCTASK = 3;
    private final int LOAD_NEIGHBORHOODS_ASYNCTASK = 4;

    private boolean enabledGps = false;
    private boolean findButton = false;

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;
    private Toolbar toolbar;
    private PolygonOptions polygonOptions;

    private EditText latEditText;
    private EditText longEditText;
    private EditText priceEditText;
    private EditText roomsEditText;
    private EditText builtAreaEditText;

    Handler handler;

    private LoadPointsPolygonAsyntask loadPolygonAsynctask;
    private LoadDepsAsynctask depsAsynctask;
    private LoadMunicipalityAsynctask munsAsynctask;
    private LoadSectorsAsynctask secsAsynctask;
    private LoadNeighborhoodsAsynctask neighsAsynctask;
    private PostItemAsyncTask postItemAsyncTask;

    private List<Department> departments;
    private List<Municipality> municipalities;
    private List<Sector> sectors;
    private List<Neighborhood> neighborhoods;

    private Spinner departmentsSpinner;
    private Spinner municipalitiesSpinner;
    private Spinner sectorsSpinner;
    private Spinner neighborhoodsSpinner;
    private Spinner typeSpinner;
    private Spinner typeAdSpinner;
    private Spinner stratumSpinner;
    private Spinner coinSpinner;
    private Spinner measureTypeSpinner;
    private Spinner typeParkingSpinner;
    private Spinner parkingConditionsSpinner;
    

    private ImageView findLatLongImageView;
    private Button postButton;

    private int intDepartment;
    private int intMunicipality;
    private int intSector;
    private int intNeigborhood;

    private String[] typeValues;
    private String[] typeAdValues;
    private String[] stratumValues;
    private String[] coinValues;
    private String[] measureValues;

    private static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        MapboxAccountManager.start(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the account manager
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        locationServices = LocationServices.getLocationServices(MainActivity.this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
            }
        });

        typeValues = getResources().getStringArray(R.array.tipo_de_propiedad_values);
        typeAdValues = getResources().getStringArray(R.array.tipo_de_anuncio_values);
        stratumValues = getResources().getStringArray(R.array.estrato_values);
        coinValues = getResources().getStringArray(R.array.tipo_de_divisa_values);
        measureValues = getResources().getStringArray(R.array.tipo_de_medida_values);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });

        departmentsSpinner = (Spinner) findViewById(R.id.spinner_departments);
        departmentsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i!=0) {
                    Log.e(LOG_TAG,departments.get(i-1).getDataName());
                    if (munsAsynctask!=null) {
                        munsAsynctask.cancel(true);
                    }
                    munsAsynctask = new LoadMunicipalityAsynctask();
                    munsAsynctask.execute((Integer) departments.get(i-1).getId());
                    municipalitiesSpinner.setSelection(0,false);
                } else {
                    municipalitiesSpinner.setSelection(0,false);
                    municipalitiesSpinner.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        municipalitiesSpinner = (Spinner) findViewById(R.id.spinner_municipalities);
        municipalitiesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i!=0) {
                    Log.e(LOG_TAG,municipalities.get(i-1).getDataName());
                    if (secsAsynctask!=null) {
                        secsAsynctask.cancel(true);
                    }
                    secsAsynctask = new LoadSectorsAsynctask();
                    secsAsynctask.execute((Integer) municipalities.get(i-1).getId());
                    sectorsSpinner.setSelection(0,false);
                    sectorsSpinner.setEnabled(false);
                } else {
                    sectorsSpinner.setSelection(0,false);
                    sectorsSpinner.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sectorsSpinner = (Spinner) findViewById(R.id.spinner_sectors);
        sectorsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i!=0) {
                    Log.e(LOG_TAG,sectors.get(i-1).getDataName());
                    if (neighsAsynctask!=null) {
                        neighsAsynctask.cancel(true);
                    }
                    neighsAsynctask = new LoadNeighborhoodsAsynctask();
                    neighsAsynctask.execute(municipalities.get(municipalitiesSpinner.getSelectedItemPosition()-1).getId());
                    neighborhoodsSpinner.setSelection(0,false);
                    neighborhoodsSpinner.setEnabled(false);
                } else {
                    neighborhoodsSpinner.setSelection(0,false);
                    neighborhoodsSpinner.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        neighborhoodsSpinner = (Spinner) findViewById(R.id.spinner_neighborhoods);
        neighborhoodsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        typeSpinner = (Spinner) findViewById(R.id.type_spinner);
        typeAdSpinner = (Spinner) findViewById(R.id.type_ad_spinner);
        stratumSpinner = (Spinner) findViewById(R.id.stratum_spinner);
        coinSpinner = (Spinner) findViewById(R.id.coin_spinner);
        measureTypeSpinner = (Spinner) findViewById(R.id.measure_type_spinner);
        typeParkingSpinner = (Spinner) findViewById(R.id.type_parking_spinner);
        parkingConditionsSpinner = (Spinner) findViewById(R.id.parking_conditions_spinner);

        latEditText = (EditText) findViewById(R.id.latEditText);
        longEditText = (EditText) findViewById(R.id.longEditText);
        priceEditText = (EditText) findViewById(R.id.price_edit_text);
        roomsEditText = (EditText) findViewById(R.id.rooms_edit_text);
        builtAreaEditText = (EditText) findViewById(R.id.rooms_edit_text);


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.e(LOG_TAG,"Manejando mensaje");

                switch (msg.arg1) {

                    case LOAD_POLYGON_ASYNCTASK:
                        drawPolygon((List<LatLng>) msg.obj);
                        Log.e(LOG_TAG,String.valueOf(intDepartment));
                        departmentsSpinner.setSelection(0,false);
                        if (enabledGps || findButton) {
                            departmentsSpinner.setSelection(getSelection(LOAD_DEPARTMENTS_ASYNCTASK));
                            intDepartment = 0;
                        }
                        break;

                    case LOAD_DEPARTMENTS_ASYNCTASK:
                        swapAdapterDepartmentSpinner((List<String>) msg.obj);
                        if (enabledGps || findButton) {
                            departmentsSpinner.setSelection(getSelection(LOAD_DEPARTMENTS_ASYNCTASK));
                            intDepartment = 0;
                        }
                        break;
                    case LOAD_MUNICIPALITIES_ASYNCTASK:
                        swapAdapterMunicipalitiesSpinner((List<String>) msg.obj);
                        if (enabledGps || findButton) {
                            municipalitiesSpinner.setEnabled(!enabledGps);
                            Log.e(LOG_TAG,String.valueOf(intMunicipality));
                            municipalitiesSpinner.setSelection(getSelection(LOAD_MUNICIPALITIES_ASYNCTASK));
                            intMunicipality = 0;
                        }
                        break;
                    case LOAD_SECTORS_ASYNCTASK:
                        swapAdapterSectorsSpinner((List<String>) msg.obj);
                        if (enabledGps || findButton) {
                            sectorsSpinner.setEnabled(!enabledGps);
                            Log.e(LOG_TAG,String.valueOf(intSector));
                            sectorsSpinner.setSelection(getSelection(LOAD_SECTORS_ASYNCTASK));
                            intSector = 0;
                        }
                        break;
                    case LOAD_NEIGHBORHOODS_ASYNCTASK:
                        swapAdapterNeighborhoodsSpinner((List<String>) msg.obj);
                        if (enabledGps || findButton) {
                            neighborhoodsSpinner.setEnabled(!enabledGps);
                            Log.e(LOG_TAG,String.valueOf(intNeigborhood));
                            neighborhoodsSpinner.setSelection(getSelection(LOAD_NEIGHBORHOODS_ASYNCTASK));
                            intNeigborhood = 0;
                            findButton = false;
                        }
                        break;
                }
            }
        };

        initSpinners();

        findLatLongImageView = (ImageView) findViewById(R.id.find_lat_long);
        findLatLongImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(latEditText.getText().toString().equals("") && longEditText.getText().toString().equals(""))) {
                    LatLng latLng = new LatLng(Double.valueOf(latEditText.getText().toString()),Double.valueOf(longEditText.getText().toString()));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                    if (loadPolygonAsynctask==null) {
                        executeLoadPolygonAsynctask(longEditText.getText().toString(),latEditText.getText().toString());
                        findButton = true;
                    }
                }
            }
        });

        postButton = (Button) findViewById(R.id.post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(latEditText.getText().toString().equals("") && longEditText.getText().toString().equals(""))
                        && departmentsSpinner.getSelectedItemPosition()!=0
                        && municipalitiesSpinner.getSelectedItemPosition()!=0
                        && sectorsSpinner.getSelectedItemPosition()!=0
                        && neighborhoodsSpinner.getSelectedItemPosition()!=0
                        && typeSpinner.getSelectedItemPosition()!=0
                        && typeAdSpinner.getSelectedItemPosition()!=0) {

                    if (postItemAsyncTask==null) {
                        try {
                            String data = "latlng=" + URLEncoder.encode(longEditText.getText().toString()+","+latEditText.getText().toString(),"UTF-8")+
                                    "&"+ "deps="+URLEncoder.encode(String.valueOf(departments.get(departmentsSpinner.getSelectedItemPosition()-1).getId()),"UTF-8")+
                                    "&"+ "muns="+URLEncoder.encode(String.valueOf(municipalities.get(municipalitiesSpinner.getSelectedItemPosition()-1).getId()),"UTF-8")+
                                    "&"+ "sector="+URLEncoder.encode(String.valueOf(sectors.get(sectorsSpinner.getSelectedItemPosition()-1).getId()),"UTF-8")+
                                    "&"+ "neigh="+URLEncoder.encode(String.valueOf(neighborhoods.get(neighborhoodsSpinner.getSelectedItemPosition()-1).getId()),"UTF-8")+
                                    "&"+ "type="+URLEncoder.encode(typeValues[typeSpinner.getSelectedItemPosition()-1],"UTF-8")+
                                    "&"+ "type_ad="+URLEncoder.encode(typeAdValues[typeAdSpinner.getSelectedItemPosition()-1],"UTF-8")+
                                    "&"+ "stratum="+URLEncoder.encode(stratumValues[stratumSpinner.getSelectedItemPosition()-1],"UTF-8")+
                                    "&"+ "coin="+URLEncoder.encode(coinValues[coinSpinner.getSelectedItemPosition()-1],"UTF-8")+
                                    "&"+ "price="+URLEncoder.encode(priceEditText.getText().toString(),"UTF-8")+
                                    "&"+ "rooms="+URLEncoder.encode(roomsEditText.getText().toString(),"UTF-8")+
                                    "&"+ "elevator="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "measure="+URLEncoder.encode(measureValues[measureTypeSpinner.getSelectedItemPosition()-1],"UTF-8")+
                                    "&"+ "built_area="+URLEncoder.encode(builtAreaEditText.getText().toString(),"UTF-8")+
                                    "&"+ "parking="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "parking_amount="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "p_conditions="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "antiquity="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "t_publish="+URLEncoder.encode("","UTF-8")+
                                    "&"+ "link="+URLEncoder.encode("","UTF-8");
                            postItemAsyncTask = new PostItemAsyncTask();
                            postItemAsyncTask.execute(data);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void initSpinners() {

        // Adapter vacio para departamentos

        List<String> depVoid = new ArrayList<String>();
        depVoid.add("Cargando Departamentos...");

        ArrayAdapter<String> adapterVoidApartments = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,depVoid);

        adapterVoidApartments.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentsSpinner.setAdapter(adapterVoidApartments);
        departmentsSpinner.setEnabled(false);

        // Adapter vacio para municipios

        List<String> munsVoid = new ArrayList<String>();
        munsVoid.add("Seleccione un Municipio");

        ArrayAdapter<String> adapterVoidMunicipalities = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,munsVoid);

        adapterVoidMunicipalities.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        municipalitiesSpinner.setAdapter(adapterVoidMunicipalities);
        municipalitiesSpinner.setEnabled(false);

        // Adapter vacio para sectores

        List<String> secVoid = new ArrayList<String>();
        secVoid.add("Seleccione un Sector");

        ArrayAdapter<String> adapterVoidSectors = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,secVoid);

        adapterVoidSectors.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectorsSpinner.setAdapter(adapterVoidSectors);
        sectorsSpinner.setEnabled(false);

        // Adapter vacio para Barrios

        List<String> neighsVoid = new ArrayList<String>();
        neighsVoid.add("Seleccione un Barrio");

        ArrayAdapter<String> adapterVoidNeighs = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,neighsVoid);

        adapterVoidNeighs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        neighborhoodsSpinner.setAdapter(adapterVoidNeighs);
        neighborhoodsSpinner.setEnabled(false);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadDepsAsyncTask();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 14));
                latEditText.setText(String.format("%s",lastLocation.getLatitude()));
                longEditText.setText(String.format("%s",lastLocation.getLongitude()));
                Log.e(LOG_TAG,String.valueOf(lastLocation.getLongitude())+","+String.valueOf(lastLocation.getLatitude()));
                executeLoadPolygonAsynctask(String.valueOf(lastLocation.getLongitude()),String.valueOf(lastLocation.getLatitude()));
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 14));
                        latEditText.setText(String.format("%s",location.getLatitude()));
                        longEditText.setText(String.format("%s",location.getLongitude()));
                        Log.e(LOG_TAG,String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude()));
                        if (loadPolygonAsynctask==null) {
                            executeLoadPolygonAsynctask(String.valueOf(location.getLongitude()),String.valueOf(location.getLatitude()));
                        }
                        // locationServices.removeLocationListener(this);
                    }
                }
            });
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
            enabledGps = true;
            updateSpinners();
            latEditText.setEnabled(false);
            longEditText.setEnabled(false);
            intDepartment = 0;
            intMunicipality = 0;
            intSector = 0;
            intNeigborhood = 0;
        } else {
            enabledGps = false;
            updateSpinners();
            latEditText.setEnabled(true);
            longEditText.setEnabled(true);
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
            if (loadPolygonAsynctask !=null) {
                cancelLoadPolygonAsyncTask();
                loadPolygonAsynctask = null;
            }

            if (polygonOptions!=null) {
                map.removePolygon(polygonOptions.getPolygon());
            }
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    private void updateSpinners() {
        departmentsSpinner.setEnabled(!enabledGps);
        municipalitiesSpinner.setEnabled(!enabledGps);
        sectorsSpinner.setEnabled(!enabledGps);
        neighborhoodsSpinner.setEnabled(!enabledGps);
    }

    private void executeLoadPolygonAsynctask(String latitud, String longitude) {
        cancelLoadPolygonAsyncTask();
        loadPolygonAsynctask = new LoadPointsPolygonAsyntask();
        loadPolygonAsynctask.execute("http://virtualbienes.com/api/public/inside/"+latitud+"/"+longitude);
    }

    private void loadDepsAsyncTask() {
        if (depsAsynctask!=null) {
            depsAsynctask.cancel(true);
        }
        depsAsynctask = new LoadDepsAsynctask();
        depsAsynctask.execute();
    }

    private void swapAdapterDepartmentSpinner (List<String> deps) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,deps);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentsSpinner.setAdapter(adapter);
        departmentsSpinner.setEnabled(true);
    }

    public int getSelection(int ASYNCTASK_ID) {
        switch (ASYNCTASK_ID) {
            case LOAD_DEPARTMENTS_ASYNCTASK:
                for (int i = 0; i < departments.size(); i++) {
                    if (departments.get(i).getId()==intDepartment) {
                        return i+1;
                    }
                }
                break;
            case LOAD_MUNICIPALITIES_ASYNCTASK:
                for (int i = 0; i < departments.size(); i++) {
                    if (municipalities.get(i).getId()==intMunicipality) {
                        return i+1;
                    }
                }
                break;
            case LOAD_SECTORS_ASYNCTASK:
                for (int i = 0; i < sectors.size(); i++) {
                    if (sectors.get(i).getId()==intSector) {
                        return i+1;
                    }
                }
                break;
            case LOAD_NEIGHBORHOODS_ASYNCTASK:
                for (int i = 0; i < neighborhoods.size(); i++) {
                    if (neighborhoods.get(i).getId()==intNeigborhood) {
                        return i+1;
                    }
                }
                break;

        }

        return 0;
    }

    private void swapAdapterMunicipalitiesSpinner (List<String> muns) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,muns);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        municipalitiesSpinner.setAdapter(adapter);
    }

    private void swapAdapterSectorsSpinner (List<String> secs) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,secs);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectorsSpinner.setAdapter(adapter);
    }

    private void swapAdapterNeighborhoodsSpinner (List<String> neighs) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,neighs);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        neighborhoodsSpinner.setAdapter(adapter);
    }

    private void cancelLoadPolygonAsyncTask() {
        if (loadPolygonAsynctask != null) {
            loadPolygonAsynctask.cancel(true);
        }
    }

    private void drawPolygon(List<LatLng> polygon) {

        if (polygonOptions!=null) {
            map.removePolygon(polygonOptions.getPolygon());
        }

        loadPolygonAsynctask = null;

        Log.e(LOG_TAG,"Poligono Cargado");

        polygonOptions = new PolygonOptions().addAll(polygon).fillColor(Color.parseColor("#ff0000")).alpha((float) 0.3);

        map.addPolygon(polygonOptions);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }

    private class LoadPointsPolygonAsyntask extends AsyncTask<String,Void,List<LatLng>> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        private String polygonJsonStr = null;

        @Override
        protected List<LatLng> doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e(LOG_TAG,"Cargando Coordenadas");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                polygonJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return getPolygonDataFromJson(polygonJsonStr);
        }

        @Override
        protected void onCancelled(List<LatLng> latLngs) {
            super.onCancelled(latLngs);
            Log.e(LOG_TAG,"AsyncTask Cancelado");
        }

        private List<LatLng> getPolygonDataFromJson(String polygonJsonStr) {

            List<LatLng> polygon = new ArrayList<>();

            final String OWM_MESSAGE_CODE = "message";
            final String OWM_INSIDE = "inside";
            final String OWM_ID = "id";
            final String OWM_DATA_NAME = "data_name";
            final String OWM_POINT = "point";
            final String OWM_DEP = "dep";
            final String OWM_MUN = "mun";
            final String OWM_SEC = "sec";
            final String OWM_BOUNDARY = "boundary";

            try {
                    JSONObject polygonJson = new JSONObject(polygonJsonStr);

                if (polygonJson.has(OWM_MESSAGE_CODE)) {
                    Log.e(LOG_TAG,"JSON Recibido correctamente");
                }

                JSONArray insideArray = polygonJson.getJSONArray(OWM_INSIDE);

                JSONObject insideJsonObject =  insideArray.getJSONObject(0);


                intNeigborhood = insideJsonObject.getInt(OWM_ID);
                String data_name = insideJsonObject.getString(OWM_DATA_NAME);
                String point = insideJsonObject.getString(OWM_POINT);
                intDepartment = insideJsonObject.getInt(OWM_DEP);
                intMunicipality = insideJsonObject.getInt(OWM_MUN);
                intSector = insideJsonObject.getInt(OWM_SEC);
                String boundary = insideJsonObject.getString(OWM_BOUNDARY);

                Log.e(LOG_TAG,boundary);

                WKTReader wktReader = new WKTReader();
                Coordinate[] polygonCoordinates;
                Geometry geometry = null;

                if (boundary!=null) {
                    try {
                        geometry = wktReader.read(boundary);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                polygonCoordinates = geometry.getCoordinates();

                for (Coordinate coordinate: polygonCoordinates) {
                    polygon.add(new LatLng(coordinate.y,coordinate.x));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return polygon;
        }

        @Override
        protected void onPostExecute(List<LatLng> latLngs) {
            super.onPostExecute(latLngs);
            final Message msg = new Message();
            msg.arg1 = LOAD_POLYGON_ASYNCTASK;
            msg.obj = latLngs;
            handler.dispatchMessage(msg);
        }
    }

    private class LoadDepsAsynctask extends AsyncTask<Void,Void,List<String>> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        private String depsJsonStr = null;

        @Override
        protected List<String> doInBackground(Void... voids) {
            try {
                URL url = new URL("http://virtualbienes.com/api/public/deps_mobile");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e(LOG_TAG,"Cargando Departamentos");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                depsJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return getDepsFromJson(depsJsonStr);
        }

        public List<String> getDepsFromJson(String depsJsonStr) {
            List<String> deps = new ArrayList<>();
            deps.add("Seleccione un Departamento");
            departments = new ArrayList<>();

            final String OWM_MESSAGE = "message";
            final String OWM_DEPS = "deps";
            final String OWM_ID = "id";
            final String OWM_DATA_NAME = "data_name";
            final String OWM_POINT = "point";

            try {
                if (depsJsonStr!=null) {
                    JSONObject depsJsonObject = new JSONObject(depsJsonStr);

                    String messageObject = depsJsonObject.getString(OWM_MESSAGE);

                    if (messageObject.equals("True")) {
                        Log.e(LOG_TAG,"Departamentos cargados correctamente");
                    } else {
                        Log.e(LOG_TAG,"Error al leer el JSON");
                    }

                    JSONArray depsArray = depsJsonObject.getJSONArray(OWM_DEPS);

                    for (int i = 0;i<depsArray.length();i++) {
                        JSONObject depJsonObject = depsArray.getJSONObject(i);
                        departments.add(new Department(
                                depJsonObject.getInt(OWM_ID),
                                depJsonObject.getString(OWM_DATA_NAME),
                                depJsonObject.getString(OWM_POINT)));

                        deps.add(depJsonObject.getString(OWM_DATA_NAME));
                    }
                } else {
                    return deps;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return deps;
        }

        @Override
        protected void onPostExecute(List<String> departments) {
            super.onPostExecute(departments);
            final Message msg = new Message();
            msg.arg1 = LOAD_DEPARTMENTS_ASYNCTASK;
            msg.obj = departments;
            handler.dispatchMessage(msg);
        }
    }

    private class LoadMunicipalityAsynctask extends AsyncTask<Integer,Void,List<String>> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        private String municipalitiesJsonStr = null;

        @Override
        protected List<String> doInBackground(Integer... integers) {
            try {
                URL url = new URL("http://virtualbienes.com/api/public/muns/"+integers[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e(LOG_TAG,"Cargando Municipios");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                municipalitiesJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return getMunicipalitiesFromJson(municipalitiesJsonStr);
        }

        public List<String> getMunicipalitiesFromJson(String municipalitiesJsonStr) {
            List<String> muns = new ArrayList<>();
            muns.add("Seleccione un Municipio");
            municipalities = new ArrayList<>();

            final String OWM_MESSAGE = "message";
            final String OWM_MUNS = "muns";
            final String OWM_ID = "id";
            final String OWM_DATA_NAME = "data_name";
            final String OWM_POINT = "point";
            final String OWM_ISO = "iso";
            final String OWM_ID_DEP = "id_dep";
            final String OWM_POSTAL_CODE = "postal_code";
            final String OWM_ZONE_POSTAL = "zone_postal";
            final String OWM_DEP = "dep";

            try {
                JSONObject municipalitiesJsonObject = new JSONObject(municipalitiesJsonStr);

                String messageObject = municipalitiesJsonObject.getString(OWM_MESSAGE);

                if (messageObject.equals("True")) {
                    Log.e(LOG_TAG,"Departamentos cargados correctamente");
                } else {
                    Log.e(LOG_TAG,"Error al leer el JSON");
                }

                JSONArray munsArray = municipalitiesJsonObject.getJSONArray(OWM_MUNS);

                for (int i = 0;i<munsArray.length();i++) {
                    JSONObject munJsonObject = munsArray.getJSONObject(i);
                    municipalities.add(new Municipality(
                            munJsonObject.getInt(OWM_ID),
                            munJsonObject.getString(OWM_DATA_NAME),
                            munJsonObject.getString(OWM_POINT),
                            munJsonObject.getString(OWM_ISO),
                            munJsonObject.getInt(OWM_ID_DEP),
                            munJsonObject.getString(OWM_POSTAL_CODE),
                            munJsonObject.getString(OWM_ZONE_POSTAL),
                            munJsonObject.getString(OWM_DEP)));

                    muns.add(munJsonObject.getString(OWM_DATA_NAME));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return muns;
        }

        @Override
        protected void onPostExecute(List<String> municipalities) {
            super.onPostExecute(municipalities);
            final Message msg = new Message();
            msg.arg1 = LOAD_MUNICIPALITIES_ASYNCTASK;
            msg.obj = municipalities;
            handler.dispatchMessage(msg);
        }
    }

    private class LoadSectorsAsynctask extends AsyncTask<Integer,Void,List<String>> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        private String sectorsJsonStr = null;

        @Override
        protected List<String> doInBackground(Integer... integers) {
            try {
                URL url = new URL("http://virtualbienes.com/api/public/secs_mobile/"+integers[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e(LOG_TAG,"Cargando Sectores");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                sectorsJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return getMunicipalitiesFromJson(sectorsJsonStr);
        }

        public List<String> getMunicipalitiesFromJson(String sectorsJsonStr) {
            List<String> secs = new ArrayList<>();
            secs.add("Seleccione un Sector");
            sectors = new ArrayList<>();

            final String OWM_MESSAGE = "message";
            final String OWM_SECS = "secs";
            final String OWM_ID = "id";
            final String OWM_DATA_NAME = "data_name";
            final String OWM_POINT = "point";

            try {
                JSONObject sectorsJsonObject = new JSONObject(sectorsJsonStr);

                String messageObject = sectorsJsonObject.getString(OWM_MESSAGE);

                if (messageObject.equals("True")) {
                    Log.e(LOG_TAG,"Sectores cargados correctamente");
                } else {
                    Log.e(LOG_TAG,"Error al leer el JSON");
                }

                JSONArray secsArray = sectorsJsonObject.getJSONArray(OWM_SECS);

                for (int i = 0;i<secsArray.length();i++) {
                    JSONObject secJsonObject = secsArray.getJSONObject(i);
                    sectors.add(new Sector(
                            secJsonObject.getInt(OWM_ID),
                            secJsonObject.getString(OWM_DATA_NAME),
                            secJsonObject.getString(OWM_POINT)));

                    secs.add(secJsonObject.getString(OWM_DATA_NAME));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return secs;
        }

        @Override
        protected void onPostExecute(List<String> sectors) {
            super.onPostExecute(sectors);
            final Message msg = new Message();
            msg.arg1 = LOAD_SECTORS_ASYNCTASK;
            msg.obj = sectors;
            handler.dispatchMessage(msg);
        }
    }

    private class LoadNeighborhoodsAsynctask extends AsyncTask<Integer,Void,List<String>> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        private String neighborhoodsJsonStr = null;

        @Override
        protected List<String> doInBackground(Integer... integers) {
            try {
                URL url = new URL("http://virtualbienes.com/api/public/neighs_op/"+integers[0]);
                Log.e(LOG_TAG,url.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e(LOG_TAG,"Cargando Barrios");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                neighborhoodsJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return getNeighborhoodsFromJson(neighborhoodsJsonStr);
        }

        public List<String> getNeighborhoodsFromJson(String neighborhoodsJsonStr) {
            List<String> neigs = new ArrayList<>();
            neigs.add("Seleccione un Barrio");
            neighborhoods = new ArrayList<>();

            final String OWM_MESSAGE = "message";
            final String OWM_NEIGHS = "neighs";
            final String OWM_ID = "id";
            final String OWM_REFERENTIAL = "referential";
            final String OWM_DATA_NAME = "data_name";

            try {
                JSONObject neighborhoodsJsonObject = new JSONObject(neighborhoodsJsonStr);

                String messageObject = neighborhoodsJsonObject.getString(OWM_MESSAGE);

                if (messageObject.equals("True")) {
                    Log.e(LOG_TAG,"Barrios cargados correctamente");
                } else {
                    Log.e(LOG_TAG,"Error al leer el JSON");
                }

                JSONArray neighsArray = neighborhoodsJsonObject.getJSONArray(OWM_NEIGHS);

                for (int i = 0;i<neighsArray.length();i++) {
                    JSONObject neighJsonObject = neighsArray.getJSONObject(i);
                    neighborhoods.add(new Neighborhood(
                            neighJsonObject.getInt(OWM_ID),
                            neighJsonObject.getString(OWM_DATA_NAME)));

                    neigs.add(neighJsonObject.getString(OWM_DATA_NAME));

                    // Log.e(LOG_TAG,neigs.get(i).toString()+neighJsonObject.getString(OWM_ID));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return neigs;
        }

        @Override
        protected void onPostExecute(List<String> neighborhoods) {
            super.onPostExecute(neighborhoods);
            final Message msg = new Message();
            msg.arg1 = LOAD_NEIGHBORHOODS_ASYNCTASK;
            msg.obj = neighborhoods;
            handler.dispatchMessage(msg);
        }
    }

    private class PostItemAsyncTask extends AsyncTask<String,Void,Void> {

        private HttpURLConnection urlConnection = null;

        private String responseJsonStr = null;

        @Override
        protected Void doInBackground(String... strings) {

            String data = strings[0];

            Log.e(LOG_TAG,data);

            try {
                URL url = new URL("http://virtualbienes.com/api/public/p_estadisticas");

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setDoOutput(true);

                urlConnection.setFixedLengthStreamingMode(data.getBytes().length);

                // urlConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                out.write(data.getBytes());
                out.flush();
                out.close();

                responseJsonStr = urlConnection.getResponseMessage();

                Log.e(LOG_TAG,responseJsonStr);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection!=null){
                    urlConnection.disconnect();
                }
            }
            return null;
        }
    }
}