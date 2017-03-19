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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final String LOG_TAG = this.getClass().getSimpleName();
    private final int LOAD_POLYGON_ASYNCTASK = 0;
    private final int LOAD_DEPARTMENTS_ASYNCTASK = 1;
    private final int LOAD_MUNICIPALITIES_ASYNCTASK = 2;
    private final int LOAD_SECTORS_ASYNCTASK = 3;
    private final int LOAD_NEIGHBORHOODS_ASYNCTASK = 4;


    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;
    private Toolbar toolbar;
    private PolygonOptions polygonOptions;

    private EditText latEditText;
    private EditText longEditText;

    Handler handler;

    private LoadPointsPolygonAsyntask loadPolygonAsynctask;
    private LoadDepsAsynctask depsAsynctask;
    private LoadMunicipalityAsynctask munsAsynctask;
    private LoadSectorsAsynctask secsAsynctask;
    private LoadNeighborhoodsAsynctask neighsAsynctask;

    private List<Department> departments;
    private List<Municipality> municipalities;
    private List<Sector> sectors;
    private List<Neighborhood> neighborhoods;

    private Spinner departmentsSpinner;
    private Spinner municipalitiesSpinner;
    private Spinner sectorsSpinner;
    private Spinner neighborhoodsSpinner;

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
                    neighsAsynctask.execute((Integer) sectors.get(i-1).getId());
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

        latEditText = (EditText) findViewById(R.id.latEditText);
        longEditText = (EditText) findViewById(R.id.longEditText);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.e(LOG_TAG,"Manejando mensaje");

                switch (msg.arg1) {
                    case LOAD_POLYGON_ASYNCTASK:
                        drawPolygon((List<LatLng>) msg.obj);
                        break;
                    case LOAD_DEPARTMENTS_ASYNCTASK:
                        swapAdapterDepartmentSpinner((List<String>) msg.obj);
                        break;
                    case LOAD_MUNICIPALITIES_ASYNCTASK:
                        swapAdapterMunicipalitiesSpinner((List<String>) msg.obj);
                        municipalitiesSpinner.setEnabled(true);
                        break;
                    case LOAD_SECTORS_ASYNCTASK:
                        swapAdapterSectorsSpinner((List<String>) msg.obj);
                        sectorsSpinner.setEnabled(true);
                        break;
                    case LOAD_NEIGHBORHOODS_ASYNCTASK:
                        swapAdapterNeighborhoodsSpinner((List<String>) msg.obj);
                        neighborhoodsSpinner.setEnabled(true);
                        break;
                }
            }
        };

        initSpinners();
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
        } else {
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


                String id = insideJsonObject.getString(OWM_ID);
                String data_name = insideJsonObject.getString(OWM_DATA_NAME);
                String point = insideJsonObject.getString(OWM_POINT);
                String dep = insideJsonObject.getString(OWM_DEP);
                String mun = insideJsonObject.getString(OWM_MUN);
                String sec = insideJsonObject.getString(OWM_SEC);
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

                        Log.e(LOG_TAG,deps.get(i).toString());
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

                    Log.e(LOG_TAG,muns.get(i).toString());
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

                    Log.e(LOG_TAG,secs.get(i).toString());
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

                    Log.e(LOG_TAG,neigs.get(i).toString());
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

}