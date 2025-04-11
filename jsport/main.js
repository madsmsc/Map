//
// parser
// 

const data = {
    polys: [],   // ??
    names: [],   // [""]
    visited: [], // {country: "", cities: [""]}
    xrange: 0,
    yrange: 0
};

const parseNames = (text) => {
    data.names.push(text.split('\n'));
    return true;
};

const parseVisited = (text) => {
    text.split('#')
        .filter((country) => country.length > 2)
        .forEach((country) => {
            const cs = country.split('|');
            if (cs.length < 2) return;
            data.visited.push({
                country: cs[0],
                cities: cs // should remove first element here
            });
        })
    return true;
};

const parseShape = (bin) => {
    // turn bin into arraybuffer somehow?
    //shapefileInput.files[0].arrayBuffer()
    ShapefileJS.Shapefile.load(bin)
        .then(shapefile => {
            console.log(shapefile.contents)
        });
    return true;
};

const parseFiles = () => {
    // does fetch work? or use this?
    // const fs = require('fs');
    // const reader = new FileReader();
    fetch("../data/names.txt")
        .then((res) => res.text()) // single string split by \n ?
        .then((text) => parseNames(text))
        .catch((e) => console.error(e));

    fetch("../data/visited.txt")
        .then((res) => res.text())
        .then((text) => parseVisited(text))
        .catch((e) => console.error(e));

    fetch("../data/large.zip")
        .then((res) => res.text()) // but it isn't text? // comment this out?
        .then((bin) => parseShape(bin))
        .catch((e) => console.error(e));
};

//
// WEBGL
//

let gl = null;
let glCanvas = null;

// Aspect ratio and coordinate system details

let aspectRatio;
let currentRotation = [0, 1];
let currentScale = [1.0, 1.0];

// Vertex information

let vertexArray;
let vertexBuffer;
let vertexNumComponents;
let vertexCount;

// Rendering data shared with the scalers

let uScalingFactor;
let uGlobalColor;
let uRotationVector;
let aVertexPosition;

// Animation timing

let shaderProgram;
let currentAngle;
let previousTime = 0.0;
let degreesPerSecond = 90.0;

function webglStartup() {
    glCanvas = document.getElementById("gl-canvas");
    gl = glCanvas.getContext("webgl");

    const shaderSet = [
        { type: gl.VERTEX_SHADER, id: "vertex-shader" },
        { type: gl.FRAGMENT_SHADER, id: "fragment-shader" }
    ];
    shaderProgram = buildShaderProgram(shaderSet);

    aspectRatio = glCanvas.width / glCanvas.height;
    currentRotation = [0, 1];
    currentScale = [1.0, aspectRatio];

    vertexArray = new Float32Array([
        -0.5, 0.5, 0.5, 0.5, 0.5, -0.5,
        -0.5, 0.5, 0.5, -0.5, -0.5, -0.5
    ]);

    vertexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, vertexArray, gl.STATIC_DRAW);

    vertexNumComponents = 2;
    vertexCount = vertexArray.length / vertexNumComponents;
    currentAngle = 0.0;
    animateScene();
}

function buildShaderProgram(shaderInfo) {
    const program = gl.createProgram();

    shaderInfo.forEach((desc) => {
        const shader = compileShader(desc.id, desc.type);

        if (shader) {
            gl.attachShader(program, shader);
        }
    });

    gl.linkProgram(program);

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        console.log("Error linking shader program:");
        console.log(gl.getProgramInfoLog(program));
    }

    return program;
}

function compileShader(id, type) {
    const code = document.getElementById(id).firstChild.nodeValue;
    const shader = gl.createShader(type);

    gl.shaderSource(shader, code);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.log(
            `Error compiling ${type === gl.VERTEX_SHADER ? "vertex" : "fragment"
            } shader:`,
        );
        console.log(gl.getShaderInfoLog(shader));
    }
    return shader;
}

let squareColor = [0.1, 0.7, 0.2, 1.0];

function animateScene() {
    gl.viewport(0, 0, glCanvas.width, glCanvas.height);
    gl.clearColor(0.8, 0.9, 1.0, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT);

    const radians = (currentAngle * Math.PI) / 180.0;
    currentRotation[0] = Math.sin(radians);
    currentRotation[1] = Math.cos(radians);

    gl.useProgram(shaderProgram);

    uScalingFactor = gl.getUniformLocation(shaderProgram, "uScalingFactor");
    uGlobalColor = gl.getUniformLocation(shaderProgram, "uGlobalColor");
    uRotationVector = gl.getUniformLocation(shaderProgram, "uRotationVector");

    gl.uniform2fv(uScalingFactor, currentScale);
    gl.uniform2fv(uRotationVector, currentRotation);
    gl.uniform4fv(uGlobalColor, squareColor);

    gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer);

    aVertexPosition = gl.getAttribLocation(shaderProgram, "aVertexPosition");

    gl.enableVertexAttribArray(aVertexPosition);
    gl.vertexAttribPointer(
        aVertexPosition,
        vertexNumComponents,
        gl.FLOAT,
        false,
        0,
        0,
    );

    gl.drawArrays(gl.TRIANGLES, 0, vertexCount);

    requestAnimationFrame((currentTime) => {
        const deltaAngle =
            ((currentTime - previousTime) / 1000.0) * degreesPerSecond;

        currentAngle = (currentAngle + deltaAngle) % 360;

        previousTime = currentTime;
        animateScene();
    });
}

//
// UI
//

let greenBlueToggle = false;
const buttons = [
    { id: 'show-data', fn: () => { } },
    { id: 'map-view', fn: () => { } },
    { id: 'text-view', fn: () => { } },
    { id: 'map-text-view', fn: () => { } },
    {
        id: 'greyscale', fn: () => {
            squareColor = [0.4, 0.4, 0.4, 1.0];
        }
    },
    {
        id: 'blue-green', fn: () => {
            greenBlueToggle = !greenBlueToggle;
            squareColor = greenBlueToggle ? [0.1, 0.2, 0.7, 1.0] : [0.1, 0.7, 0.2, 1.0];
        }
    }];
const offset = 30;
let bnum = 0;

buttons.forEach((b) => {
    // render
    document.write('<div id="' + b.id + '" class="button">' + b.id + '</div>');
    // register
    const button = document.getElementById(b.id);
    button.addEventListener('click', () => {
        button.style.color = 'purple';
        button.style.textDecoration = 'underline';
        b.fn();
    });
    // move
    button.style.top = (offset * bnum++ + 8) + 'px';
});

// 
// startup
//

const startup = () => {
    // move somewhere else? 
    // and what about sync? 
    // this needs to happen before rendering.
    parseFiles();
    // webglStartup();
};

window.addEventListener("load", startup, false);
