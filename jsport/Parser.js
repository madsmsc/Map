export class Parser {
    constructor(fileName) {
        this.polys = [];
        this.xrange = [0, 0];
        this.yrange = [0, 0];
        this._offset = 0;
        // Start loading the file using fetch
        this.ready = Promise.all([
            fetch(fileName).then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.arrayBuffer();
            }),
            fetch('../data/names.txt').then(res => res.text()),
            fetch('../data/visited.txt').then(res => res.text())
        ])
            .then(([arrayBuffer, namesText, visitedText]) => {
                this.dataView = new DataView(arrayBuffer);
                this.parseHeader();
                this.polys = this.parseObjects();

                // Parse names and visited
                const names = namesText.split(/\r?\n/).filter(line => line.length > 0);
                const visitedArr = visitedText.split(/\r?\n/).filter(line => line.length > 0);
                const info = this.parseVisited(visitedArr);

                this.postParsing(this.polys, names, visitedArr, info);
            })
            .catch(error => {
                console.error('Error loading file:', error);
            });
    }

    parseVisited(visited) {
        let info = [];
        let tmp = [];
        let vstr = '';
        for (const str of visited) vstr += str;
        const countries = vstr.split('#');
        for (const country of countries) {
            if (country === countries[0]) continue;
            const cities = country.split('|');
            tmp.push(cities[0].trim());
            info.push(country);
        }
        visited.length = 0;
        for (const s of tmp) visited.push(s);
        return info;
    }

    parseHeader() {
        let offset = 0;
        const getIntBE = () => { const v = this.dataView.getInt32(offset, false); offset += 4; return v; };
        const getIntLE = () => { const v = this.dataView.getInt32(offset, true); offset += 4; return v; };
        const getDoubleLE = () => { const v = this.dataView.getFloat64(offset, true); offset += 8; return v; };

        const fileCode = getIntBE();
        for (let i = 0; i < 5; i++) getIntBE();
        const fileLength = getIntBE();
        const version = getIntLE();
        const shapeType = getIntLE();
        const minX = getDoubleLE();
        const minY = getDoubleLE();
        const maxX = getDoubleLE();
        const maxY = getDoubleLE();
        const minZ = getDoubleLE();
        const maxZ = getDoubleLE();
        const minM = getDoubleLE();
        const maxM = getDoubleLE();
        this.xrange = [minX, maxX];
        this.yrange = [minY, maxY];
        this._offset = offset;
        console.log(`File length: ${fileLength}
Version: ${version}
Shape type: ${shapeType}
MBR: ${parseInt(minX)}, ${parseInt(minY)}, ${parseInt(maxX)}, ${parseInt(maxY)}`);
    }

    parseObjects() {
        let offset = this._offset;
        const polys = [];
        for (let r = 0; r < 100000; r++) {
            try {
                const poly = new Poly();
                poly.number = this.dataView.getInt32(offset, true); offset += 4;
                poly.length = this.dataView.getInt32(offset, true); offset += 4;
                poly.type = this.dataView.getInt32(offset, true); offset += 4;
                poly.minX = this.dataView.getFloat64(offset, true); offset += 8;
                poly.minY = this.dataView.getFloat64(offset, true); offset += 8;
                poly.maxX = this.dataView.getFloat64(offset, true); offset += 8;
                poly.maxY = this.dataView.getFloat64(offset, true); offset += 8;

                // Read number of parts and points
                const numParts = this.dataView.getInt32(offset, true); offset += 4;
                const numPoints = this.dataView.getInt32(offset, true); offset += 4;

                // Read parts array
                for (let i = 0; i < numParts; i++) {
                    poly.parts.push(this.dataView.getInt32(offset, true));
                    offset += 4;
                }

                // Read points array
                for (let i = 0; i < numPoints; i++) {
                    const x = this.dataView.getFloat64(offset, true); offset += 8;
                    const y = this.dataView.getFloat64(offset, true); offset += 8;
                    poly.points.push({ x, y });
                }

                polys.push(poly);
            } catch (e) {
                break;
            }
        }
        this._offset = offset;
        console.log(`${polys.length} objects read (max 100k)`);
        return polys;
    }

    postParsing(polys, names, visited, info) {
        if (polys.length !== names.length) {
            console.log('Shapefile does not match names.txt.');
            return;
        }
        for (let i = 0; i < polys.length; i++) {
            polys[i].name = names[i];
            polys[i].cities = [];
            const index = visited.indexOf(names[i]);
            if (index === -1) continue;
            const cities = info[index].split('|');
            for (const city of cities) {
                if (city === cities[0]) continue;
                const str = city.split('*');
                if (str.length === 4) {
                    const x = parseFloat(str[1]);
                    const y = parseFloat(str[2]);
                    polys[i].cities.push({
                        name: str[0],
                        x: x,
                        y: y,
                        info: str[3]
                    });
                } else if (str.length === 2) {
                    polys[i].cities.push({
                        name: str[0],
                        x: 0,
                        y: 0,
                        info: str[1]
                    });
                }
            }
        }
        // Optionally print info
        // let sameType = true;
        // const whichType = polys[0].type;
        // for (let i = 0; i < polys.length; i++)
        //     if (polys[i].type !== whichType) sameType = false;
        // console.log(`Are all objects of type ${whichType}? ${sameType ? 'YES' : 'NO'}`);
        // console.log('> starting gui.');
    }
}

class Poly {
    constructor() {
        this.number = 0;
        this.length = 0;
        this.type = 0;
        this.minX = 0;
        this.minY = 0;
        this.maxX = 0;
        this.maxY = 0;
        this.parts = [];
        this.points = [];
        this.center = { x: 0, y: 0 };
        this.name = '';
        this.cities = [];
    }
}
