export class Render {
    glCanvas = null;
    gl = null;
    shaderProgram = null;
    aVertexPosition = null;
    uZoom = null;
    uPan = null;
    polys = [];
    xrange = [0, 1];
    yrange = [0, 1];
    zoom = 1.0;
    pan = { x: 0.0, y: 0.0 };
    isDragging = false;
    lastMouse = { x: 0, y: 0 };

    // Add color definitions similar to ColorScheme in Map.java
    colorWater = [0.3, 0.7, 1.0, 1.0];
    colorLand = [0.8, 0.8, 0.7, 1.0];
    colorVisited = [0.6, 0.9, 0.6, 1.0];
    colorBorder = [0.1, 0.1, 0.1, 1.0];
    colorSelected = [0.6, 0.6, 0.6, 1.0]; // grey

    selectedPoly = null; // Track the selected polygon

    constructor(polys, xrange, yrange) {
        this.glCanvas = document.getElementById("gl-canvas");
        this.gl = this.glCanvas.getContext("webgl");
        this.shaderProgram = this.createTestShaderProgram();
        this.aVertexPosition = this.gl.getAttribLocation(this.shaderProgram, "aVertexPosition");
        this.uZoom = this.gl.getUniformLocation(this.shaderProgram, "uZoom");
        this.uPan = this.gl.getUniformLocation(this.shaderProgram, "uPan");
        this.polys = polys;
        this.xrange = xrange;
        this.yrange = yrange;

        this.setupZoomPanHandlers();
    }

    render() {
        this.renderPolys(this.polys, this.xrange, this.yrange);
    }

    normalizePoint(x, y, xrange, yrange, aspectRatio = 1) {
        let nx = 2 * (x - xrange[0]) / (xrange[1] - xrange[0]) - 1;
        let ny = 2 * (y - yrange[0]) / (yrange[1] - yrange[0]) - 1;
        nx = nx / aspectRatio;
        return [nx, ny];
    }

    polysToVertices(polys, xrange, yrange, aspectRatio = 1) {
        // Returns {vertices: Float32Array, polyOffsets: Array}
        const vertices = [];
        const polyOffsets = [];
        let offset = 0;
        polys.forEach(poly => {
            polyOffsets.push({ offset, length: poly.points.length });
            for (let i = 0; i < poly.points.length; i++) {
                const pt = poly.points[i];
                const [nx, ny] = this.normalizePoint(pt.x, pt.y, xrange, yrange, aspectRatio);
                vertices.push(nx, ny);
                offset++;
            }
        });
        return { vertices: new Float32Array(vertices), polyOffsets };
    }

    // Extracted helper for fill color selection
    getFillColor(poly) {
        if (this.selectedPoly === poly) {
            return this.colorSelected;
        } else if (poly.cities && poly.cities.length > 0) {
            return this.colorVisited;
        } else {
            return this.colorLand;
        }
    }

    // Helper to get the actual number of points to draw for a ring
    getActualRingLength(poly, start, end) {
        let ringLen = end - start;
        if (ringLen < 3) return 0; // Not enough points for a polygon

        const first = poly.points[start];
        const last = poly.points[end - 1];
        let actualLen = ringLen;
        if (ringLen > 3 && first.x === last.x && first.y === last.y) {
            actualLen -= 1;
        }
        if (actualLen < 3) return 0;
        return actualLen;
    }

    // Add this helper inside your Render class:
    getConvexRingIndices(poly, start, end) {
        // Returns the indices for a convex ring, skipping duplicate closing point if present
        let ringLen = end - start;
        if (ringLen < 3) return null;
        const first = poly.points[start];
        const last = poly.points[end - 1];
        let actualLen = ringLen;
        if (ringLen > 3 && first.x === last.x && first.y === last.y) {
            actualLen -= 1;
        }
        if (actualLen < 3) return null;
        return { start, count: actualLen };
    }

    renderPolys(polys, xrange, yrange) {
        const aspect = this.glCanvas.width / this.glCanvas.height;
        const { vertices } = this.polysToVertices(polys, xrange, yrange, aspect);

        const buffer = this.gl.createBuffer();
        this.gl.bindBuffer(this.gl.ARRAY_BUFFER, buffer);
        this.gl.bufferData(this.gl.ARRAY_BUFFER, vertices, this.gl.STATIC_DRAW);

        // Draw water background
        this.gl.clearColor(...this.colorWater);
        this.gl.clear(this.gl.COLOR_BUFFER_BIT);

        this.gl.useProgram(this.shaderProgram);
        this.gl.enableVertexAttribArray(this.aVertexPosition);
        this.gl.vertexAttribPointer(this.aVertexPosition, 2, this.gl.FLOAT, false, 0, 0);

        // Set uniforms for zoom and pan
        this.gl.uniform1f(this.uZoom, this.zoom);
        this.gl.uniform2f(this.uPan, this.pan.x, this.pan.y);

        let globalOffset = 0;
        polys.forEach(poly => {
            let fillColor = this.getFillColor(poly);

            if (poly.parts && poly.parts.length > 0) {
                // --- Modified loop using getConvexRingIndices ---
                for (let p = 0; p < poly.parts.length; p++) {
                    const start = poly.parts[p];
                    const end = (p + 1 < poly.parts.length) ? poly.parts[p + 1] : poly.points.length;
                    const ring = this.getConvexRingIndices(poly, start, end);
                    if (!ring) continue;

                    // Draw filled polygon (convex only)
                    this.setFillColor(fillColor);
                    this.gl.drawArrays(this.gl.TRIANGLE_FAN, globalOffset + ring.start, ring.count);

                    // Draw border
                    this.setFillColor(this.colorBorder);
                    this.gl.drawArrays(this.gl.LINE_LOOP, globalOffset + ring.start, ring.count);
                }
                globalOffset += poly.points.length;
            }
        });

        const err = this.gl.getError();
        if (err !== this.gl.NO_ERROR) {
            console.error('WebGL error:', err);
        }
    }

    // Helper to set fill color uniform (adds uFillColor if needed)
    setFillColor(color) {
        if (!this.uFillColor) {
            this.uFillColor = this.gl.getUniformLocation(this.shaderProgram, "uFillColor");
        }
        this.gl.uniform4fv(this.uFillColor, color);
    }

    // Extracted from setupZoomPanHandlers
    handlePolygonClick(e) {
        // Convert mouse to normalized device coordinates
        const rect = this.glCanvas.getBoundingClientRect();
        const x = ((e.clientX - rect.left) / this.glCanvas.width) * 2 - 1;
        const y = -(((e.clientY - rect.top) / this.glCanvas.height) * 2 - 1);

        // Undo pan/zoom/aspect normalization
        const aspect = this.glCanvas.width / this.glCanvas.height;
        const nx = x * aspect / this.zoom - this.pan.x;
        const ny = y / this.zoom - this.pan.y;

        // Map back to world coordinates
        const wx = (nx + 1) * (this.xrange[1] - this.xrange[0]) / 2 + this.xrange[0];
        const wy = (ny + 1) * (this.yrange[1] - this.yrange[0]) / 2 + this.yrange[0];

        // Find the polygon that contains the point, checking each part separately
        const found = this.polys.find(poly => {
            if (poly.parts && poly.parts.length > 0) {
                for (let p = 0; p < poly.parts.length; p++) {
                    const start = poly.parts[p];
                    const end = (p + 1 < poly.parts.length) ? poly.parts[p + 1] : poly.points.length;
                    const ring = poly.points.slice(start, end);
                    if (this.pointInPoly({ x: wx, y: wy }, ring)) {
                        return true;
                    }
                }
            }
            return false;
        });
        if (!found) {
            console.log('-- no polygon found --');
        } else {
            let desc = `--\n-- ${found.name}\n--`;
            found.cities?.forEach(city => {
                desc += `\n${city.name} --${city.info}\n`;
            });
            console.log(desc);
        }
        // Set the selected polygon and re-render
        this.selectedPoly = found || null;
        this.render();
    }

    setupZoomPanHandlers() {
        this.glCanvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const zoomFactor = 1.1;
            if (e.deltaY < 0) {
                this.zoom *= zoomFactor;
            } else {
                this.zoom /= zoomFactor;
            }
            this.render();
        });

        this.glCanvas.addEventListener('mousedown', (e) => {
            this.isDragging = true;
            this.lastMouse.x = e.clientX;
            this.lastMouse.y = e.clientY;
        });

        window.addEventListener('mousemove', (e) => {
            if (this.isDragging) {
                const dx = (e.clientX - this.lastMouse.x) / (this.glCanvas.width / 2);
                const dy = (e.clientY - this.lastMouse.y) / (this.glCanvas.height / 2);
                this.pan.x += dx / this.zoom;
                this.pan.y -= dy / this.zoom;
                this.lastMouse.x = e.clientX;
                this.lastMouse.y = e.clientY;
                this.render();
            }
        });

        window.addEventListener('mouseup', () => {
            this.isDragging = false;
        });

        // --- Polygon click handler ---
        this.glCanvas.addEventListener('click', this.handlePolygonClick.bind(this));
    }

    // Ray-casting algorithm for point-in-polygon
    pointInPoly(point, vs) {
        let x = point.x, y = point.y;
        let inside = false;
        for (let i = 0, j = vs.length - 1; i < vs.length; j = i++) {
            let xi = vs[i].x, yi = vs[i].y;
            let xj = vs[j].x, yj = vs[j].y;
            let intersect = ((yi > y) !== (yj > y)) &&
                (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    createTestShaderProgram() {
        const vsSource = `
        attribute vec2 aVertexPosition;
        uniform float uZoom;
        uniform vec2 uPan;
        void main(void) {
            vec2 pos = (aVertexPosition + uPan) * uZoom;
            gl_Position = vec4(pos, 0.0, 1.0);
        }`;
        const fsSource = `
        precision mediump float;
        uniform vec4 uFillColor;
        void main(void) {
            gl_FragColor = uFillColor;
        }`;
        function createShader(gl, type, source) {
            const shader = gl.createShader(type);
            gl.shaderSource(shader, source);
            gl.compileShader(shader);
            if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
                console.error('Shader compile failed:', gl.getShaderInfoLog(shader));
                gl.deleteShader(shader);
                return null;
            }
            return shader;
        }
        const vertexShader = createShader(this.gl, this.gl.VERTEX_SHADER, vsSource);
        const fragmentShader = createShader(this.gl, this.gl.FRAGMENT_SHADER, fsSource);
        const program = this.gl.createProgram();
        this.gl.attachShader(program, vertexShader);
        this.gl.attachShader(program, fragmentShader);
        this.gl.linkProgram(program);
        if (!this.gl.getProgramParameter(program, this.gl.LINK_STATUS)) {
            console.error('Program failed to link:', this.gl.getProgramInfoLog(program));
            return null;
        }
        return program;
    }
}
