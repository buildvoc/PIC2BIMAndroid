
var constNumber = 0;
var svidPrefix = "";
var graphTitle = "";

var layout = {
    title: {
        text:'SKYVIEW',
        font: {
          family: 'Arial',
          size: 18
        },
        xref: 'paper',
        x: 0.05,
    },
    margin: {
        l: 25,
        r: 25,
        b: 15,
        t: 50,
        pad: 4
      },
    polar: {
        angularaxis: {
            tickfont: {
              size: 12
            },
            rotation: 90,
            direction: "clockwise",
            tickmode: "array",
            tickvals: [0, 45, 90, 135, 180, 225, 270, 315 ],
            ticktext: ["N","NE","E", "SE", "S", "SW","W","NW"]
        },
        radialaxis: {
          visible: true,
          showline: false,
          showticklabels: false,
          showgrid: true,
          showexponent: "none",
          showtickprefix: "none",
          thetaunit: "degrees",
          range: [90, 0],
          side:"clockwise"
        }
    },
    showlegend: false
}

let dataArray = [{
    type: "scatterpolargl",
    mode: "markers+text",
    r: 1, //elevation
    theta: 1, //azimuth
    name: "-",
    marker: {
        color: "rgba(255,0,0,0.6)",
        size: 1,
        line: {
          color: "black"
        }
    },
    text: "-",
    textfont:{
        color:"black",
        size:10,
        family:"Arial"
    }
}];

setTimeout(()=>{
    Plotly.newPlot('skymap', dataArray, layout, {displayModeBar: false});
},100);


var updateGraph = function(base64Data, remoteBase64Data, constType, onlyActual){

    if(onlyActual == "true"){
        document.getElementById("expectedLegend").style.display="none";
    }else{
        document.getElementById("expectedLegend").style.display="block";
    }

    jsondata = JSON.parse(atob(base64Data));
    //console.log(jsondata);

    remoteJsonData = JSON.parse(atob(remoteBase64Data));

    let dataArray = [];
    if(onlyActual == "false"){
        for (let satname in remoteJsonData) {
                let satObj = remoteJsonData[satname];
                let dataObj = {
                    type: "scatterpolargl",
                    mode: "markers+text",
                    r:[parseFloat(satObj.elevation).toFixed(1)], //elevation
                    theta:[parseFloat(satObj.azimuth).toFixed(1)], //azimuth
                    name: satname,
                    marker: {
                        color: "rgba(255,0,0,0.6)",
                        size: 20,
                        line: {
                          color: "black"
                        }
                    },
                    text: satname,
                    textfont:{
                        color:"black",
                        size:10,
                        family:"Arial"
                    }
                };
                dataArray.push(dataObj);
            }
    }


    /*
            public static final int CONSTELLATION_BEIDOU = 5;
            public static final int CONSTELLATION_GALILEO = 6;
            public static final int CONSTELLATION_GLONASS = 3;
            public static final int CONSTELLATION_GPS = 1;
            public static final int CONSTELLATION_IRNSS = 7;
            public static final int CONSTELLATION_QZSS = 4;
            public static final int CONSTELLATION_SBAS = 2;
     */
     graphTitle = constType;
     if(constType == "GPS"){
         constNumber = 1;
         svidPrefix = "G";
     }else if(constType == "GLONASS"){
         svidPrefix = "";
         constNumber = 3;
     }else if(constType == "BEIDOU"){
        svidPrefix = "C";
         constNumber = 5;
     }else if(constType == "GALILEO"){
         constNumber = 6;
         svidPrefix = "E";
     }
     layout.title.text = graphTitle;
     //{svid: '3', azimuth: '183.0', elevation: '5.0', constellation: 3, snr: 31.88066291809082}

    if(jsondata.length == 0 && onlyActual == "true"){
        dataArray = [{
            type: "scatterpolargl",
            mode: "markers+text",
            r: 1, //elevation
            theta: 1, //azimuth
            name: "-",
            marker: {
                color: "rgba(255,0,0,0.6)",
                size: 1,
                line: {
                  color: "black"
                }
            },
            text: "-",
            textfont:{
                color:"black",
                size:10,
                family:"Arial"
            }
        }];
    }
    for(var i = 0; i < jsondata.length; i++) {
        let obj = jsondata[i];

        if( obj.constellation == constNumber ){
            let dataObj = {
                type: "scatterpolargl",
                mode: "markers+text",
                r:[parseFloat(obj.elevation).toFixed(1)], //elevation
                theta:[parseFloat(obj.azimuth).toFixed(1)], //azimuth
                name: svidPrefix+obj.svid,
                marker: {
                    color: "rgba(0,0,255,0.6)",
                    size: 20,
                    line: {
                      color: "black"
                    }
                },
                text: svidPrefix+obj.svid,
                textfont:{
                    color:"white",
                    size:10,
                    family:"Arial"
                }
              };
              dataArray.push(dataObj);
        }
    }

    Plotly.newPlot('skymap', dataArray, layout, {displayModeBar: false, staticPlot: true});
}