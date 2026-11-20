package ace.eis.com.gps;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button datos;
    EditText latitud;
    EditText longitud;
    TextView resultado;
    ObtenerWebService hiloconexion;
    Location location;
    LocationManager locationManager;
    LocationListener locationListener;
    AlertDialog alert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        datos = (Button) findViewById(R.id.datos);
        latitud = (EditText) findViewById(R.id.latitud);
        longitud = (EditText) findViewById(R.id.longitud);
        resultado = (TextView) findViewById(R.id.resultado);

        datos.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            resultado.setText("Servicio de ubicación no disponible en este dispositivo.");
            return;
        }

        /****Si no esta activado el GPS lo enciende****/
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertNoGps();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        1001);
                return;
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        MostrarLocalizacion(location);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                MostrarLocalizacion(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener);


        /* Datos de los proveedores */
        //List<String> listaProveedores = locationManager.getAllProviders();
        //LocationProvider proveedor1 = locationManager.getProvider(listaProveedores.get(0));
        //int Accuracy = proveedor1.getAccuracy();
        //boolean TieneAltitud = proveedor1.supportsAltitude();
        /*----*/

        /* Establezco criterios para buscar un proveedor */
        //Criteria criteria = new Criteria();
        //criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //String mejorProveedor = locationManager.getBestProvider(criteria,true);
        /*-----*/


    }

    private void AlertNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(alert != null)
        {
            alert.dismiss ();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            } else {
                locationManager.removeUpdates(locationListener);
            }
        } else {
            locationManager.removeUpdates(locationListener);
        }


    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }



    }

    public void MostrarLocalizacion(Location loc){



        if (loc!=null){
            hiloconexion = new ObtenerWebService();
            hiloconexion.execute(String.valueOf(loc.getLatitude()),String.valueOf(loc.getLongitude()));
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        if (hiloconexion != null && hiloconexion.getStatus() != AsyncTask.Status.FINISHED) {
            hiloconexion.cancel(true);
        }
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException ignored) {
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.datos) {
            String latText = latitud.getText().toString().trim().replace(',', '.');
            String lonText = longitud.getText().toString().trim().replace(',', '.');
            try {
                double latVal = Double.parseDouble(latText);
                double lonVal = Double.parseDouble(lonText);
                if (latVal < -90.0 || latVal > 90.0 || lonVal < -180.0 || lonVal > 180.0) {
                    resultado.setText("Coordenadas fuera de rango (lat: -90..90, lon: -180..180).");
                    return;
                }
                hiloconexion = new ObtenerWebService();
                hiloconexion.execute(String.valueOf(latVal), String.valueOf(lonVal));
            } catch (NumberFormatException ex) {
                resultado.setText("Ingrese latitud y longitud numéricas válidas.");
            }
        }
    }

    public class ObtenerWebService extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            if (params == null || params.length < 2) {
                return "Faltan coordenadas para consultar.";
            }

            String cadena = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + params[0] + "," + params[1];
            String devuelve = "";
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(cadena);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "Android-GPS-Client/1.0");

                int respuesta = connection.getResponseCode();
                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject respuestaJSON = new JSONObject(result.toString());
                    JSONArray resultJSON = respuestaJSON.optJSONArray("results");

                    String direccion = "SIN DATOS PARA ESA LONGITUD Y LATITUD";
                    if (resultJSON != null && resultJSON.length() > 0) {
                        direccion = resultJSON.getJSONObject(0).optString("formatted_address", direccion);
                    }
                    devuelve = "Dirección: " + direccion;
                } else {
                    devuelve = "Error HTTP al consultar geocodificación (" + respuesta + ")";
                }
            } catch (MalformedURLException e) {
                devuelve = "URL inválida: " + e.getMessage();
            } catch (IOException e) {
                devuelve = "Error de red: " + e.getMessage();
            } catch (JSONException e) {
                devuelve = "Error procesando respuesta JSON: " + e.getMessage();
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException ignored) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return devuelve;
        }

        @Override
        protected void onCancelled(String aVoid) {
            super.onCancelled(aVoid);
        }

        @Override
        protected void onPostExecute(String aVoid) {
            resultado.setText(aVoid);
            Log.i("GPS",aVoid);
            //super.onPostExecute(aVoid);
        }

        @Override
        protected void onPreExecute() {
            resultado.setText("");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }


}
