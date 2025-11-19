export class Parser {
    constructor() {
        this.polys = [];
        this.xrange = [0, 0];
        this.yrange = [0, 0];
        this.offset = 0;
        this.ready = this.parseFiles()
    }

    parseFiles() {
        return Promise.all([
            fetch('../data/large.shp').then(response => {
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
                const names = namesText.split(/\r?\n/).filter(line => line.length > 0);
                const visitedArr = visitedText.split(/\r?\n/).filter(line => line.length > 0);
                const info = this.parseVisited(visitedArr);
                this.postParsing(names, visitedArr, info);
            })
            .catch(error => {
                console.error('Error loading file:', error);
            });
    }

    parseVisited(visited) {
        let fullCountryStrings = [];
        const countries = visited.join('').split('#').splice(1);
        visited.length = 0; // changed to just country names
        for (const country of countries) {
            visited.push(country.split('|')[0].trim());
            fullCountryStrings.push(country);
        }
        return fullCountryStrings;
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
        this.offset = offset;
        console.log(`File length: ${fileLength}
Version: ${version}
Shape type: ${shapeType}
MBR: ${parseInt(minX)}, ${parseInt(minY)}, ${parseInt(maxX)}, ${parseInt(maxY)}`);
    }

    parseObjects() {
        let offset = this.offset;
        const polys = [];
        for (let r = 0; r < 100000; r++) {
            try {
                offset = this.parsePoly(offset, polys);
            } catch (e) {
                break; // parse up to 100k lines or until error
            }
        }
        this.offset = offset;
        console.log(`${polys.length} objects read (max 100k)`);
        return polys;
    }

    parsePoly(offset, polys) {
        const poly = new Poly();
        poly.number = this.dataView.getInt32(offset, true); offset += 4;
        poly.length = this.dataView.getInt32(offset, true); offset += 4;
        poly.type = this.dataView.getInt32(offset, true); offset += 4;
        poly.minX = this.dataView.getFloat64(offset, true); offset += 8;
        poly.minY = this.dataView.getFloat64(offset, true); offset += 8;
        poly.maxX = this.dataView.getFloat64(offset, true); offset += 8;
        poly.maxY = this.dataView.getFloat64(offset, true); offset += 8;
        const numParts = this.dataView.getInt32(offset, true); offset += 4;
        const numPoints = this.dataView.getInt32(offset, true); offset += 4;
        for (let i = 0; i < numParts; i++) { // parts array
            poly.parts.push(this.dataView.getInt32(offset, true));
            offset += 4;
        }
        for (let i = 0; i < numPoints; i++) { // points array
            const x = this.dataView.getFloat64(offset, true); offset += 8;
            const y = this.dataView.getFloat64(offset, true); offset += 8;
            poly.points.push({ x, y });
        }
        polys.push(poly);
        return offset;
    }

    postParsing(names, visited, info) {
        console.log(this.polys);
        if (this.polys.length !== names.length) {
            console.log('Shapefile does not match names.txt.');
            return;
        }
        for (let i = 0; i < this.polys.length; i++) {
            this.polys[i].name = names[i];
            this.polys[i].cities = [];
            const index = visited.indexOf(names[i]);
            if (index === -1) continue;
            info[index].split('|').splice(1).forEach(c => this.postParsingCity(c, this.polys[i]));
        }
    }

    postParsingCity(city, poly) {
        const str = city.split('*');
        if (str.length === 4) {
            poly.cities.push({
                name: str[0],
                x: parseFloat(str[1]),
                y: parseFloat(str[2]),
                info: str[3]
            });
        } else if (str.length === 2) {
            poly.cities.push({
                name: str[0],
                x: 0,
                y: 0,
                info: str[1]
            });
        }
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
