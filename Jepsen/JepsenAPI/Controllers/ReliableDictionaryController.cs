// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace JepsenAPI.Controllers
{
    using System;
    using System.Collections.Generic;
    using System.Fabric;
    using System.Fabric.Query;
    using System.Linq;
    using System.Net.Http;
    using System.Net.Http.Headers;
    using System.Text;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Newtonsoft.Json;

    [Produces("application/json")]
    [Route("api/[controller]")]
    public class ReliableDictionaryController : Controller
    {
        private readonly HttpClient httpClient;
        private readonly FabricClient fabricClient;
        private readonly string reverseProxyBaseUri;
        private readonly StatelessServiceContext serviceContext;

        public ReliableDictionaryController(HttpClient httpClient, StatelessServiceContext context, FabricClient fabricClient)
        {
            this.fabricClient = fabricClient;
            this.httpClient = httpClient;
            this.serviceContext = context;
            this.reverseProxyBaseUri = Environment.GetEnvironmentVariable("ReverseProxyBaseUri");
        }



        // GET: api/ReliableDictionary
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);

            ServicePartitionList partitions = await this.fabricClient.QueryManager.GetPartitionListAsync(serviceName);

            List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();

            result.Add(new KeyValuePair<String, int>($"{serviceName}", 1));
            result.Add(new KeyValuePair<String, int>($"{proxyAddress}", 2));

            foreach (Partition partition in partitions)
            {
                string proxyUrl = $"{proxyAddress}api/ReliableDictionary?PartitionKey={((Int64RangePartitionInformation)partition.PartitionInformation).LowKey}&PartitionKind=Int64Range";
                result.Add(new KeyValuePair<String, int>(proxyUrl, 3));
                using (HttpResponseMessage response = await this.httpClient.GetAsync(proxyUrl))
                {
                    if (response.StatusCode != System.Net.HttpStatusCode.OK)
                    {
                        continue;
                    }

                    result.AddRange(JsonConvert.DeserializeObject<List<KeyValuePair<string, int>>>(await response.Content.ReadAsStringAsync()));
                }
            }

            

            return this.Json(result);
        }

        //// GET: api/Votes
        //[HttpGet("")]
        //public async Task<IActionResult> Get()
        //{
        //    Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
        //    Uri proxyAddress = this.GetProxyAddress(serviceName);

        //    ServicePartitionList partitions = await this.fabricClient.QueryManager.GetPartitionListAsync(serviceName);

        //    List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();

        //    foreach (Partition partition in partitions)
        //    {
        //        string proxyUrl =
        //            $"{proxyAddress}/api/ReliableDictionary?PartitionKey={((Int64RangePartitionInformation)partition.PartitionInformation).LowKey}&PartitionKind=Int64Range";

        //        using (HttpResponseMessage response = await this.httpClient.GetAsync(proxyUrl))
        //        {
        //            if (response.StatusCode != System.Net.HttpStatusCode.OK)
        //            {
        //                continue;
        //            }

        //            result.AddRange(JsonConvert.DeserializeObject<List<KeyValuePair<string, int>>>(await response.Content.ReadAsStringAsync()));
        //        }
        //    }

        //    return this.Json(result);
        //}

        // GET: api/Votes
        [HttpGet("{key}")]
        public async Task<IActionResult> Get(string key)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);

            ServicePartitionList partitions = await this.fabricClient.QueryManager.GetPartitionListAsync(serviceName);

            List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();

            foreach (Partition partition in partitions)
            {
                string proxyUrl =
                    $"{proxyAddress}api/ReliableDictionary/{key}?PartitionKey={partitionKey}&PartitionKind=Int64Range";

                using (HttpResponseMessage response = await this.httpClient.GetAsync(proxyUrl))
                {
                    if (response.StatusCode != System.Net.HttpStatusCode.OK)
                    {
                        continue;
                    }

                    result.AddRange(JsonConvert.DeserializeObject<List<KeyValuePair<string, int>>>(await response.Content.ReadAsStringAsync()));
                }
            }

            return this.Json(result);
        }

        // PUT: api/Votes/name
        [HttpPut("{key}")]
        public async Task<IActionResult> Put(string key)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);
            string proxyUrl = $"{proxyAddress}api/ReliableDictionary/{key}?PartitionKey={partitionKey}&PartitionKind=Int64Range";

            StringContent putContent = new StringContent($"{{ 'name' : '{key}' }}", Encoding.UTF8, "application/json");
            putContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");

            using (HttpResponseMessage response = await this.httpClient.PutAsync(proxyUrl, putContent))
            {
                return new ContentResult()
                {
                    StatusCode = (int)response.StatusCode,
                    Content = await response.Content.ReadAsStringAsync()
                };
            }
        }

        // PUT: api/Votes/name
        [HttpPut("{key}/{value}")]
        public async Task<IActionResult> Put(string key, int value)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);
            string proxyUrl = $"{proxyAddress}api/ReliableDictionary/{key}/{value}?PartitionKey={partitionKey}&PartitionKind=Int64Range";

            StringContent putContent = new StringContent($"{{ 'name' : '{key}' }}", Encoding.UTF8, "application/json");
            putContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");

            using (HttpResponseMessage response = await this.httpClient.PutAsync(proxyUrl, putContent))
            {
                return new ContentResult()
                {
                    StatusCode = (int)response.StatusCode,
                    Content = await response.Content.ReadAsStringAsync()
                };
            }
        }

        // PUT: api/Votes/name
        [HttpPut("{key}​/{value}​/{exspected}")]
        public async Task<IActionResult> Put(string key, int value, int exspected)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);
            string proxyUrl = $"{proxyAddress}api/ReliableDictionary/{key}/{value}/{exspected}/?PartitionKey={partitionKey}&PartitionKind=Int64Range";

            StringContent putContent = new StringContent($"{{ 'key' : '{key}' }}", Encoding.UTF8, "application/json");
            putContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");

            using (HttpResponseMessage response = await this.httpClient.PutAsync(proxyUrl, putContent))
            {
                return new ContentResult()
                {
                    StatusCode = (int)response.StatusCode,
                    Content = await response.Content.ReadAsStringAsync()
                };
            }
        }

        // DELETE: api/Votes/name
        [HttpDelete("{key}")]
        public async Task<IActionResult> Delete(string key)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);
            string proxyUrl = $"{proxyAddress}api/ReliableDictionary/{key}?PartitionKey={partitionKey}&PartitionKind=Int64Range";

            using (HttpResponseMessage response = await this.httpClient.DeleteAsync(proxyUrl))
            {
                if (response.StatusCode != System.Net.HttpStatusCode.OK)
                {
                    return this.StatusCode((int)response.StatusCode);
                }
            }

            return new OkResult();
        }


        /// <summary>
        /// Constructs a reverse proxy URL for a given service.
        /// </summary>
        /// <param name="serviceName"></param>
        /// <returns></returns>
        private Uri GetProxyAddress(Uri serviceName)
        {
            return new Uri($"{this.reverseProxyBaseUri}");
            //return new Uri($"{this.reverseProxyBaseUri}{serviceName.AbsolutePath}");
        }

        /// <summary>
        /// Creates a partition key from the given name.
        /// Uses the zero-based numeric position in the alphabet of the first letter of the name (0-25).
        /// </summary>
        /// <param name="name"></param>
        /// <returns></returns>
        private long GetPartitionKey(string name)
        {
            return Char.ToUpper(name.First()) - 'A';
        }
    }
}